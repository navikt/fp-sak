package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.util.List;

public class SubmissionData<T> {

    private T data;
    private List<Attachment> attachments;

    public SubmissionData() {}

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
}
