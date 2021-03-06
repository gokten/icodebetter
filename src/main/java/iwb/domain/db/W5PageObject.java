package iwb.domain.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Immutable;

import iwb.util.GenericUtil;


@Entity
@Immutable
@Table(name="w5_template_object",schema="iwb")
public class W5PageObject implements java.io.Serializable, W5Base {


	private static final long serialVersionUID = 77712365423231L;
	private int templateObjectId;
	private int templateId;
	private int parentObjectId;
	private int objectId;
	
	private short objectTip;

	private short tabOrder;
	private short activeFlag;
	
	private short accessViewTip; //0:kisitlama yok, 1:var
	private String accessViewRoles;
	private String accessViewUsers;
	private String postJsCode;
	
	private Integer srcQueryFieldId;
	private Integer dstQueryParamId;
	private Integer dstStaticQueryParamId;
	private String dstStaticQueryParamVal;

	private String _srcQueryFieldName;
	private String _dstQueryParamName;
	private String _dstStaticQueryParamName;


	@Id
	@Column(name="template_object_id")
	public int getTemplateObjectId() {
		return templateObjectId;
	}

	public void setTemplateObjectId(int templateObjectId) {
		this.templateObjectId = templateObjectId;
	}
	
	@Column(name="template_id")	
	public int getTemplateId() {
		return templateId;
	}

	public void setTemplateId(int templateId) {
		this.templateId = templateId;
	}


	@Column(name="object_id")
	public int getObjectId() {
		return objectId;
	}

	public void setObjectId(int objectId) {
		this.objectId = objectId;
	}

	@Column(name="object_tip")	
	public short getObjectTip() {
		return objectTip;
	}

	public void setObjectTip(short objectTip) {
		this.objectTip = objectTip;
	}

	@Column(name="tab_order")	
	public short getTabOrder() {
		return tabOrder;
	}

	public void setTabOrder(short tabOrder) {
		this.tabOrder = tabOrder;
	}


	@Column(name="access_view_tip")
	public short getAccessViewTip() {
		return accessViewTip;
	}

	@Column(name="access_view_roles")
	public String getAccessViewRoles() {
		return accessViewRoles;
	}

	@Column(name="access_view_users")
	public String getAccessViewUsers() {
		return accessViewUsers;
	}

	@Column(name="post_js_code")
	public String getPostJsCode() {
		return postJsCode;
	}

	public void setPostJsCode(String postJsCode) {
		this.postJsCode = postJsCode;
	}

	public void setAccessViewTip(short accessViewTip) {
		this.accessViewTip = accessViewTip;
	}

	public void setAccessViewRoles(String accessViewRoles) {
		this.accessViewRoles = accessViewRoles;
	}

	public void setAccessViewUsers(String accessViewUsers) {
		this.accessViewUsers = accessViewUsers;
	}
	
	@Transient
	public boolean safeEquals(W5Base q){
		if(q==null)return false;
		W5PageObject t = (W5PageObject)q;
		return 	
				this.templateObjectId == t.getTemplateObjectId()  &&
		this.templateId == t.getTemplateId() &&

		this.objectId == t.getObjectId()  &&
		
		this.objectTip == t.getObjectTip()  &&

		this.tabOrder == t.getTabOrder()  &&
		
		this.accessViewTip == t.getAccessViewTip()  && //0:kisitlama yok, 1:var
		GenericUtil.safeEquals(this.accessViewRoles, t.getAccessViewRoles()) &&
		GenericUtil.safeEquals(this.accessViewUsers, t.getAccessViewUsers())
//		 && PromisUtil.safeEquals(this.postJsCode, t.getPostJsCode())
		;
	}
	

	@Column(name="parent_object_id")
	public int getParentObjectId() {
		return parentObjectId;
	}

	public void setParentObjectId(int parentObjectId) {
		this.parentObjectId = parentObjectId;
	}

	@Column(name="src_query_field_id")
	public Integer getSrcQueryFieldId() {
		return srcQueryFieldId;
	}

	public void setSrcQueryFieldId(Integer srcQueryFieldId) {
		this.srcQueryFieldId = srcQueryFieldId;
	}

	@Column(name="dst_query_param_id")
	public Integer getDstQueryParamId() {
		return dstQueryParamId;
	}

	public void setDstQueryParamId(Integer dstQueryParamId) {
		this.dstQueryParamId = dstQueryParamId;
	}

	@Column(name="dst_static_query_param_id")
	public Integer getDstStaticQueryParamId() {
		return dstStaticQueryParamId;
	}

	public void setDstStaticQueryParamId(Integer dstStaticQueryParamId) {
		this.dstStaticQueryParamId = dstStaticQueryParamId;
	}

	@Column(name="dst_static_query_param_val")
	public String getDstStaticQueryParamVal() {
		return dstStaticQueryParamVal;
	}

	public void setDstStaticQueryParamVal(String dstStaticQueryParamVal) {
		this.dstStaticQueryParamVal = dstStaticQueryParamVal;
	}

	@Transient
	public String get_srcQueryFieldName() {
		return _srcQueryFieldName;
	}

	public void set_srcQueryFieldName(String _srcQueryFieldName) {
		this._srcQueryFieldName = _srcQueryFieldName;
	}

	@Transient
	public String get_dstQueryParamName() {
		return _dstQueryParamName;
	}

	public void set_dstQueryParamName(String _dstQueryParamName) {
		this._dstQueryParamName = _dstQueryParamName;
	}

	@Transient
	public String get_dstStaticQueryParamName() {
		return _dstStaticQueryParamName;
	}

	public void set_dstStaticQueryParamName(String _dstStaticQueryParamName) {
		this._dstStaticQueryParamName = _dstStaticQueryParamName;
	}

	@Column(name="active_flag")
	public short getActiveFlag() {
		return activeFlag;
	}

	public void setActiveFlag(short activeFlag) {
		this.activeFlag = activeFlag;
	}	
	private String projectUuid;
	@Id
	@Column(name="project_uuid")
	public String getProjectUuid() {
		return projectUuid;
	}

	public void setProjectUuid(String projectUuid) {
		this.projectUuid = projectUuid;
	}

	
	public boolean equals(Object o) {
		if(o==null || !(o instanceof W5PageObject))return false;
		W5PageObject c = (W5PageObject)o;
		return c!=null && c.getTemplateObjectId()==getTemplateObjectId() && c.getProjectUuid().equals(projectUuid);
	}
	
	public int hashCode() {
		return projectUuid.hashCode() + 100*getTemplateObjectId();
	}	
}
