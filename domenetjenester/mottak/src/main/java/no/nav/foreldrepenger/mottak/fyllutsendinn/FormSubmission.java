package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level envelope for all FormIO nav form submissions.
 * Usage: DefaultJsonMapper.fromJson(json, new TypeReference<FormSubmission<Nav140410Data>>(){});
 */
public class FormSubmission<T> {

    private String language;
    private SubmissionData<T> data;

    public FormSubmission() {}

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public SubmissionData<T> getData() { return data; }
    public void setData(SubmissionData<T> data) { this.data = data; }
}
