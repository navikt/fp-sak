package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.folketrygdloven.kalkulus.felles.JournalpostId;
import no.nav.folketrygdloven.kalkulus.kodeverk.RefusjonKilde;
import no.nav.folketrygdloven.kalkulus.response.fordelingsgrunnlag.gui.RefusjonsperiodeDto;

@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
public record RefusjonsandelDto(
    @NotNull @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$") String arbeidsgiverIdent,
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$") String arbeidsforholdId,
    @Valid @NotNull RefusjonKilde kilde,
    @Valid JournalpostId journalpostId,
    @NotNull List<@Valid RefusjonsperiodeDto> refusjonsperioder,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Arbeidsforholdets andel er tilkommet ved periodisering eller fordeling av beregningsgrunnlaget, "
        + "som erTilkommetAndel i beregningsvisningen. Sammenlignes ikke med tidligere behandling. "
        + "False hvis beregningsgrunnlaget eller en matchende andel mangler.")
    boolean erTilkommetArbeidsforhold) {
}
