package iwb.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import iwb.cache.FrameworkCache;
import iwb.cache.FrameworkSetting;
import iwb.cache.LocaleMsgCache;
import iwb.dao.rdbms_impl.PostgreSQL;
import iwb.domain.db.W5Conversion;
import iwb.domain.db.W5ConvertedObject;
import iwb.domain.db.W5Table;
import iwb.domain.db.W5WsMethod;
import iwb.domain.helper.W5QueuedActionHelper;
import iwb.domain.result.W5FormResult;
import iwb.exception.IWBException;
import iwb.util.GenericUtil;

@Component
public class ConversionEngine {
	@Lazy
	@Autowired
	private PostgreSQL dao;

	@Lazy
	@Autowired
	private CRUDEngine crudEngine;

	@Lazy
	@Autowired
	private RESTEngine restEngine;

	public boolean extFormConversion(W5FormResult formResult, String prefix, int action, Map<String, Object> scd,
			Map<String, String> requestParams, W5Table t, String ptablePk, boolean cleanConversion) {
		if ((action == 0 || action == 1 || action == 2)
				&& !GenericUtil.isEmpty(formResult.getForm().get_conversionList())) {
			Set<Integer> cnvSet = new HashSet();
			if (!cleanConversion)
				for (W5Conversion cnv : formResult.getForm().get_conversionList())
					if (
					/* (action==1 && cnv.getSynchOnUpdateFlag()!=0) || */ ((cnv.getConversionTip() == 3 /* invisible */
							|| cnv.getConversionTip() == 0 /* kesin */)
							&& GenericUtil.hasPartInside2(cnv.getActionTips(), action))) {
						cnvSet.add(cnv.getConversionId());
					}
			String cnvStr = requestParams.get("_cnvStr");
			if (cnvStr != null) {
				String[] arCnvStr = cnvStr.split(",");
				if (arCnvStr != null && arCnvStr.length > 0)
					for (String acs : arCnvStr) {
						int conversionId = GenericUtil.uInt(acs);
						if (conversionId != 0)
							cnvSet.add(conversionId);
					}
			}

			if (action == 1 || action == 0) { // conversion olan bir form? o
												// zaman sync olan covnerted
												// objeleri bul
				String inStr = "";
				if (!cleanConversion) {
					for (W5Conversion cnv : formResult.getForm().get_conversionList())
						if (GenericUtil.hasPartInside2(cnv.getActionTips(), action)
								|| cnv.getSynchOnUpdateFlag() != 0) { // synch
																		// varsa
							inStr += "," + cnv.getConversionId();
						}
				} else
					for (Integer s : cnvSet) {
						inStr += "," + s;
					}

				if (inStr.length() > 1) {
					List<W5ConvertedObject> lco = dao.find(
							"from W5ConvertedObject x where x.projectUuid=? AND x.conversionId in ("
									+ inStr.substring(1) + ") and x.srcTablePk=?",
							scd.get("projectId"),
							GenericUtil.uInt(requestParams, t.get_tableParamList().get(0).getDsc() + prefix));
					if (!lco.isEmpty()) {
						Map<Integer, List<W5ConvertedObject>> m = new HashMap();
						formResult.setMapConvertedObject(m);
						for (W5ConvertedObject co : lco) {
							List<W5ConvertedObject> l = m.get(co.getConversionId());
							if (l == null) {
								l = new ArrayList();
								m.put(co.getConversionId(), l);
							}
							l.add(co);
							cnvSet.add(co.getConversionId());
						}
					}
				}
			}

			List<Map<String, String>> previewConversionMapList = null;
			boolean convertedAny = false;
			W5Conversion c = null;
			for (int conversionId : cnvSet)
				try {
					c = null;
					for (W5Conversion cnv : formResult.getForm().get_conversionList())
						if (cnv.getConversionId() == conversionId) {
							c = cnv;
							break;
						}
					if (c == null)
						continue; // bulamadi
					// if(!PromisUtil.hasPartInside2(c.getActionTips(), action)
					// && (action!=1 ||
					// c.getSynchOnUpdateFlag()==0)) continue; // bulamadi veya
					// buldugunun action'i uygun
					// degil
					if (c.getSrcDstTip() == 0) { // Table -> Table
						if (formResult.getMapConvertedObject() != null) {
							List<W5ConvertedObject> lco = formResult.getMapConvertedObject().get(c.getConversionId());
							if (lco != null) { // dikkat daha once convert
												// edilmis object var
								if (c.getMaxNumofConversion() == 0 || c.getMaxNumofConversion() > lco.size()
										|| (!cleanConversion && c.getSynchOnUpdateFlag() != 0)) {
									if (c.getSynchOnUpdateFlag() != 0)
										for (W5ConvertedObject co : lco) { // bu
																			// conversion'da
																			// synch
																			// var
											//
											// formResult.getOutputMessages().add(PromisLocaleMsg.get2(0,(String)scd.get("locale"),"fw_conversion_auto_error")+
											// "
											// ["+PromisLocaleMsg.get2(scd,c.getDsc())+"]");
											int advancedConditionGroupId = 0;

											String prefix2 = "";
											Map<String, String> m = new HashMap();
											m.put("a", "1");
											m.put("_fid", "" + c.getDstFormId());
											W5Table dt = FrameworkCache.getTable(scd, c.getDstTableId());
											m.put(dt.get_tableParamList().get(0).getDsc() + prefix2,
													"" + co.getDstTablePk());
											W5FormResult dstFormResult = dao.getFormResult(scd, c.getDstFormId(), 1, m);
											Map<String, Object> mq = dao.interprateConversionTemplate(c, dstFormResult,
													GenericUtil.uInt(ptablePk), false, true);
											if (GenericUtil.isEmpty(mq)) {
												formResult.getOutputMessages()
														.add(LocaleMsgCache.get2(0, (String) scd.get("locale"),
																"fw_conversion_sync_error") + "  ["
														+ LocaleMsgCache.get2(scd, c.getDsc()) + "]");
												continue;
											}
											for (String k : mq.keySet())
												if (!k.startsWith("_"))
													m.put(k + prefix2, mq.get(k).toString());

											crudEngine.postForm4Table(dstFormResult, prefix2, new HashSet());
											if (dstFormResult.getErrorMap().isEmpty()) {
												co.setVersionNo(co.getVersionNo() + 1);
												co.setVersionUserId((Integer) scd.get("userId"));
												dao.updateObject(co);

												if (FrameworkSetting.liveSyncRecord)
													formResult.addSyncRecordAll(
															dstFormResult.getListSyncAfterPostHelper());
											} else {
												// cok dogru degil
												// throw new
												// PromisException("security","Form",
												// 0, null,
												// PromisLocaleMsg.get2(0,(String)scd.get("locale"),"fw_conversion_auto_synch_error")+
												// " 2-
												// ["+PromisLocaleMsg.get2(scd,c.getDsc())+"]:
												// " +
												// PromisUtil.fromMapToHtmlString(dstFormResult.getErrorMap()),
												// null);
											}
										}
								} else {
									formResult.getOutputMessages()
											.add(LocaleMsgCache.get2(scd, "fw_conversion_msg_for") + " ["
													+ LocaleMsgCache.get2(scd, c.getDsc()) + "]: "
													+ LocaleMsgCache.get2(scd, "fw_max_number_of_conversion"));
									continue; // daha fazla conversion yok,
												// sinira ulasmis
								}
							}
						}
						if (!GenericUtil.hasPartInside2(c.getActionTips(), action))
							continue; // bulamadi veya buldugunun action'i uygun
										// degil

						Map<String, String> m = new HashMap();
						m.put("_cnvId", "" + conversionId);
						// m.put("_cnvGroupId",""+advancedConditionGroupId);
						// m.put("_cnvDetailTableIds",""+advancedConditionGroupId);
						m.put("_fid", "" + c.getDstFormId());
						m.put("_cnvTblPk", ptablePk);
						m.put("_cnvDsc", c.getDsc());
						m.put(".w", requestParams.get(".w"));
						if (c.getPreviewFlag() != 0) { // simdi gonderilmeyecek,
														// formda geri donecek
							if (previewConversionMapList == null) {
								previewConversionMapList = new ArrayList();
								formResult.setPreviewConversionMapList(previewConversionMapList);
							}
							previewConversionMapList.add(m);
							continue;
						} else {
							m.put("a", "2");
							W5FormResult dstFormResult = dao.getFormResult(scd, c.getDstFormId(), 2, requestParams);
							Map mq = dao.interprateConversionTemplate(c, dstFormResult, GenericUtil.uInt(ptablePk),
									false, false);
							if (GenericUtil.isEmpty(mq)) {
								formResult.getOutputMessages()
										.add(LocaleMsgCache.get2(0, (String) scd.get("locale"),
												"fw_conversion_auto_error") + " 1- ["
										+ LocaleMsgCache.get2(scd, c.getDsc()) + "]");
								continue;
							}
							m.putAll(mq);

							if (GenericUtil.isEmpty(c.getRhinoCode()) || c.getRhinoCode().startsWith("!")) {
								W5FormResult newFormResult = crudEngine.postForm4Table(scd, c.getDstFormId(), 2, m,
										prefix);
								if (newFormResult.getErrorMap().isEmpty()) {
									formResult.getOutputMessages()
											.add(LocaleMsgCache.get2(0, (String) scd.get("locale"),
													"fw_conversion_auto_done") + " ["
											+ LocaleMsgCache.get2(scd, c.getDsc()) + "]");
									convertedAny = true;

									if (FrameworkSetting.liveSyncRecord)
										formResult.addSyncRecordAll(newFormResult.getListSyncAfterPostHelper());
									if (!GenericUtil.isEmpty(c.getRhinoCode())) {
										if (!GenericUtil.isEmpty(newFormResult.getOutputFields()))
											for (String s : newFormResult.getOutputFields().keySet())
												if (newFormResult.getOutputFields().get(s) != null)
													m.put("out." + s,
															newFormResult.getOutputFields().get(s).toString());
										dao.executeRhinoScript(scd, requestParams, c.getRhinoCode(), m, "result");
									}
								} else {
									if (FrameworkSetting.debug)
										System.out.println(newFormResult.getErrorMap());
									if (c.getRowErrorStrategyTip() == 1)
										throw new IWBException("security", "Form", 0, null,
												LocaleMsgCache.get2(0, (String) scd.get("locale"),
														"fw_conversion_auto_error") + "  2- ["
												+ LocaleMsgCache.get2(scd, c.getDsc()) + "]", null);
									else {
										formResult.getOutputMessages()
												.add(LocaleMsgCache.get2(0, (String) scd.get("locale"),
														"fw_conversion_auto_error") + "  3- ["
												+ LocaleMsgCache.get2(scd, c.getDsc()) + "]");
										/*
										 * if(previewConversionMapList==null){
										 * //TODO aslinda hata durumunda preivew
										 * acsin mi denilebilir. bir dusun
										 * previewConversionMapList= new
										 * ArrayList(); formResult.
										 * setPreviewConversionMapList(
										 * previewConversionMapList); }
										 * previewConversionMapList.add(m);
										 */
									}
								}
							} else {
								dao.executeRhinoScript(scd, requestParams, c.getRhinoCode(), m, "result");
							}
						}
					} else if (c.getSrcDstTip() == 1) { // Table -> Ws Method
						if (formResult.getMapConvertedObject() != null) {
							List<W5ConvertedObject> lco = formResult.getMapConvertedObject().get(c.getConversionId());
							if (lco != null && c.getMaxNumofConversion() > 0
									&& c.getMaxNumofConversion() <= lco.size()) { // yapilmayacak
								continue;
							}
						}

						W5WsMethod wsm = FrameworkCache.getWsMethod(scd, c.getDstTableId());
						if (wsm.get_params() == null) {
							wsm.set_params(dao.find(
									"from W5WsMethodParam t where t.wsMethodId=? AND t.projectUuid=? order by t.tabOrder",
									wsm.getWsMethodId(), (String) scd.get("projectId")));
						}

						Map mq = dao.interprateConversionTemplate4WsMethod(scd, requestParams, c,
								GenericUtil.uInt(ptablePk), wsm);
						if (GenericUtil.isEmpty(mq)) {
							formResult.getOutputMessages()
									.add(LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_conversion_auto_error")
											+ " 1- [" + LocaleMsgCache.get2(scd, c.getDsc()) + "]");
							continue;
						}
						if (GenericUtil.isEmpty(c.getRhinoCode()) || c.getRhinoCode().startsWith("!")) {
							Map result = restEngine.REST(scd, wsm.get_ws().getDsc() + "." + wsm.getDsc(), mq); // TODO:
																									// result
																									// ne
																									// yapayim
							if (result != null) {
								if (result.containsKey("faultcode"))
									throw new IWBException("rhino", "Conversion", c.getConversionId(),
											wsm.get_ws().getDsc() + "." + wsm.getDsc(),
											result.get("faulcode") + ": " + result.get("faultstring"), null);
								else {
									if (formResult.getOutputFields() == null)
										formResult.setOutputFields(new HashMap());
									formResult.getOutputFields().putAll(result);
								}
								mq.putAll(result);
							}
						}
						if (!GenericUtil.isEmpty(c.getRhinoCode())) {
							dao.executeRhinoScript(scd, requestParams, c.getRhinoCode(), mq, "result");
						}
					}
				} catch (IWBException e) {
					if (c.getRowErrorStrategyTip() == 1 && cleanConversion) // throw
																			// e;
						throw new IWBException("framework", "Conversion", conversionId, null,
								"[707," + conversionId + "] " + c.getDsc(), e);
					// if(FrameworkSetting.debug)e.printStackTrace();
					formResult.getOutputMessages().add("CONVERSION(" + conversionId + ") EXCEPTION: " + e.getMessage());
				} catch (Exception e) {
					if (c.getRowErrorStrategyTip() == 1 && cleanConversion)
						throw new IWBException("framework", "Conversion", conversionId, null,
								"[707," + conversionId + "] " + c.getDsc(), e);
					// throw new IWBException("framework","Conversion",
					// conversionId, null,
					// LocaleMsgCache.get2(0,(String)scd.get("locale"),"fw_conversion_auto_error")+
					// " 3-
					// ["+e.getMessage()+"]", null);
					// if(FrameworkSetting.debug)e.printStackTrace();
					formResult.getOutputMessages().add("CONVERSION(" + conversionId + ") EXCEPTION: " + e.getMessage());
				}
			return convertedAny;
		} else
			return false;
	}
	
	public W5FormResult postBulkConversion(Map<String, Object> scd, int conversionId, int dirtyCount,
			Map<String, String> requestParams, String prefix) {
		W5Conversion cnv = null;
		try {
			int customizationId = (Integer) scd.get("customizationId");
			String projectId = (String) scd.get("projectId");
			if (/* customizationId > 0 && */scd != null && (Integer) scd.get("roleId") == 0)
				projectId = FrameworkCache.getProjectId(scd, "707." + conversionId);
			cnv = (W5Conversion) dao.getCustomizedObject(
					"from W5Conversion t where t.conversionId=? AND t.projectUuid=?", conversionId, projectId, null);
			if (cnv == null || GenericUtil.isEmpty(cnv.getActionTips()) || cnv.getActiveFlag() == 0)
				throw new IWBException("validation", "Conversion", conversionId, null,
						"Conversion Control Error (" + conversionId + ")", null);
			Set<String> checkedParentRecords = new HashSet();

			if (GenericUtil.hasPartInside2(cnv.getActionTips(), 0)) { // manual
																		// conversion
				int srcFormId = cnv.getSrcFormId();
				W5FormResult formResult = dao.getFormResult(scd, srcFormId, 2, requestParams);
				W5Table t = FrameworkCache.getTable(scd, formResult.getForm().getObjectId()); // formResult.getForm().get_sourceTable();
				int convertedCount = 0, errorConversionCount = 0;
				for (int id = 1; id <= dirtyCount; id++) {
					Map<String, String> m = new HashMap();
					m.put("_cnvStr", conversionId + "");
					m.put(t.get_tableParamList().get(0).getDsc(), requestParams.get("srcTablePk" + prefix + id));
					m.put(".w", requestParams.get(".w"));
					String pid = prefix + id;
					for (String k : requestParams.keySet())
						if (!k.startsWith("srcTablePk") && k.endsWith(pid)) { // veli,
																				// eger
																				// request'ten
																				// gelen
																				// extra
																				// parametreler
																				// varsa,
																				// onlari
																				// da
																				// yeni
							// map'e koy
							m.put(k.substring(0, k.length() - pid.length()), requestParams.get(k));
						}
					boolean b = extFormConversion(formResult, prefix, 0, scd, m, t,
							requestParams.get("srcTablePk" + prefix + id), true);
					if (!b || !formResult.getErrorMap().isEmpty()) {
						if (cnv.getRowErrorStrategyTip() == 1) {
							String msg = LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_conversion_auto_error")
									+ "  2- [" + LocaleMsgCache.get2(scd, cnv.getDsc()) + "]";
							if (!GenericUtil.isEmpty(formResult.getOutputMessages()))
								for (String s : formResult.getOutputMessages()) {
									msg += "<br> - " + s;
								}
							throw new IWBException("security", "Form", 0, null, msg, null);
						} else {
							//
							// formResult.getOutputMessages().add(PromisLocaleMsg.get2(0,(String)scd.get("locale"),"fw_conversion_auto_error")+
							// " 3-
							// ["+PromisLocaleMsg.get2(scd,cnv.getDsc())+"]");
							errorConversionCount++;
						}
						formResult.getErrorMap().clear();
					} else
						convertedCount++;
				}
				if (!GenericUtil.isEmpty(formResult.getOutputMessages()))
					formResult.getOutputMessages().add("-");
				formResult.getOutputMessages().add(convertedCount + " "
						+ LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_records_converted"));
				if (errorConversionCount > 0)
					formResult.getOutputMessages().add(errorConversionCount + " "
							+ LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_records_not_converted"));
				formResult.setQueuedActionList(new ArrayList<W5QueuedActionHelper>());
				return formResult;
			} else if (GenericUtil.hasPartInside2(cnv.getActionTips(), 3)) { // bulk
																				// conversion

				List<W5QueuedActionHelper> queuedGlobalFuncList = new ArrayList<W5QueuedActionHelper>();

				int dstFormId = cnv.getDstFormId();
				W5FormResult formResult = dao.getFormResult(scd, dstFormId, 2, requestParams);
				W5Table t = FrameworkCache.getTable(scd, formResult.getForm().getObjectId()); // formResult.getForm().get_sourceTable();
				if (t.getAccessViewTip() == 0 && !FrameworkCache.roleAccessControl(scd, 0)) {
					throw new IWBException("security", "Module", 0, null,
							LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_guvenlik_modul_kontrol"), null);
				}
				if (t.getAccessViewUserFields() == null && !GenericUtil.accessControl(scd, t.getAccessViewTip(),
						t.getAccessViewRoles(), t.getAccessViewUsers())) {
					throw new IWBException("security", "Form", dstFormId, null,
							LocaleMsgCache.get2(0, (String) scd.get("locale"), "fw_guvenlik_tablo_kontrol_goruntuleme"),
							null);
				}
				Map originalRequestParams = formResult.getRequestParams();
				for (int id = 1; id <= dirtyCount; id++) {
					// formResult.setAction(PromisUtil.uInt(requestParams.get("a"+prefix+id)));
					int srcTablePk = GenericUtil.uInt(requestParams, "srcTablePk" + prefix + id);
					Map mq = dao.interprateConversionTemplate(cnv, formResult, srcTablePk, false, false);
					if (!GenericUtil.isEmpty(mq)) {
						mq.put("_cnvId", "" + conversionId);
						mq.put("_cnvTblPk", "" + srcTablePk);
						formResult.setRequestParams(mq);
						queuedGlobalFuncList.addAll(crudEngine.postForm4Table(formResult, prefix, checkedParentRecords));
						formResult.setRequestParams(originalRequestParams);
						if (!formResult.getErrorMap().isEmpty()) {
							throw new IWBException("validation", "Form", dstFormId, null,
									"Detay Conversion Veri Geçerliliği("
											+ LocaleMsgCache.get2((Integer) scd.get("customizationId"),
													(String) scd.get("locale"), formResult.getForm().getLocaleMsgKey())
											+ "): " + GenericUtil.fromMapToJsonString(formResult.getErrorMap()),
									null);
						}

					} else
						throw new IWBException("validation", "Conversion", conversionId, null,
								"Detay Conversion Hatası", null);
				}

				formResult.setQueuedActionList(queuedGlobalFuncList);
				if (formResult.getOutputMessages() != null && formResult.getOutputMessages().isEmpty())
					formResult.getOutputMessages().add("Toplam " + dirtyCount + " adet işlem gerçekleşti.");
				return formResult;
			} else
				throw new IWBException("validation", "Conversion", conversionId, null,
						"Conversion Control Error2 (" + conversionId + ")", null);
		} catch (Exception e) {
			throw new IWBException("framework", "Bulk.Conversion", conversionId, "",
					"[707," + conversionId + "]" + (cnv != null ? " " + cnv.getDsc() : ""), e);
		}
	}
	
	public W5FormResult postBulkConversionMulti(Map<String, Object> scd, int conversionCount,
			Map<String, String> parameterMap) {
		W5FormResult result = null;
		for (int i = 1; i <= conversionCount; i++) {
			int cnvId = GenericUtil.uInt(parameterMap, "_cnvId" + i);
			int dirtyCount = GenericUtil.uInt(parameterMap, "_cnt" + i);

			result = postBulkConversion(scd, cnvId, dirtyCount, parameterMap, i + ".");
		}
		return result;
	}
}
