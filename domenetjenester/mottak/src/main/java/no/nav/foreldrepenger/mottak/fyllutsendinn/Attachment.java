package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.util.List;

public class Attachment {

    private String attachmentId;
    private String navId;
    /** e.g. "personal-id", "default", "other" */
    private String type;
    /** e.g. "norwegianPassport", "leggerVedNaa", "nei" */
    private String value;
    private String title;
    private List<AttachmentFile> files;

    public Attachment() {}

    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }

    public String getNavId() { return navId; }
    public void setNavId(String navId) { this.navId = navId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<AttachmentFile> getFiles() { return files; }
    public void setFiles(List<AttachmentFile> files) { this.files = files; }
}
