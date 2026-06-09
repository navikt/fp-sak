package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Top-level envelope for all FormIO nav form submissions.
 * Usage: DefaultJsonMapper.fromJson(json, new TypeReference<FormSubmission<Nav140410Data>>(){});
 */
public record FormSubmission<T>(@NotNull String language, @NotNull SubmissionData<@Valid T> data) {}
