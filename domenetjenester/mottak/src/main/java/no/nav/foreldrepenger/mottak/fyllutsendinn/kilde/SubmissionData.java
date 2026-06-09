package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Vurder å fjerne attachment. Vi har ikke bruk for det. Står i PDF og synlig som journalført vedlegg
public record SubmissionData<T>(@Valid @NotNull T data, List<@Valid Attachment> attachments) {}
