package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubmissionData<T>(@NotNull T data, List<@Valid Attachment> attachments) {}
