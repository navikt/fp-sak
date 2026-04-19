package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Attachment with metadata and optional file references. */
public record Attachment(
    @NotNull String attachmentId,
    @NotNull String navId,
    @NotNull String type,
    String value,
    String title,
    String additionalDocumentation,
    List<@Valid AttachmentFile> files
) {}
