package no.nav.foreldrepenger.mottak.fyllutsendinn;

public class AttachmentFile {

    private String fileId;
    private String attachmentId;
    private String innsendingId;
    private String fileName;
    private long size;

    public AttachmentFile() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }

    public String getInnsendingId() { return innsendingId; }
    public void setInnsendingId(String innsendingId) { this.innsendingId = innsendingId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
