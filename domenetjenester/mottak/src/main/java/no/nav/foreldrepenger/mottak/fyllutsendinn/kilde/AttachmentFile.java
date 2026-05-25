package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import jakarta.validation.constraints.NotNull;

public record AttachmentFile(
    @NotNull String fileId,
    @NotNull String attachmentId,
    @NotNull String innsendingId,
    @NotNull String fileName,
    long size
) {}
