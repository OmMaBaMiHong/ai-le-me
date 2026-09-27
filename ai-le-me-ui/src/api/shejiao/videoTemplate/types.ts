export interface VideoTemplateVO {
  id?: number;
  name?: string;
  templateId?: string;
  provider?: string;
  category?: string;
  coverImage?: string;
  previewVideo?: string;
  description?: string;
  scriptVariables?: string;
  duration?: number;
  status?: number;
  sort?: number;
  createTime?: string;
  updateTime?: string;
}

export interface VideoTemplateForm extends VideoTemplateVO {}

export interface VideoTemplateQuery extends PageQuery {
  name?: string;
  status?: number;
  provider?: string;
  category?: string;
}
