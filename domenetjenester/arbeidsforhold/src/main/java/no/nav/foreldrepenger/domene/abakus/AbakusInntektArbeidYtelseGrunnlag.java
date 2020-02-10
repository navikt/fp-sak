package no.nav.foreldrepenger.domene.abakus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

public class AbakusInntektArbeidYtelseGrunnlag extends InntektArbeidYtelseGrunnlag {

    @DiffIgnore
    private UUID koblingReferanse;

    public AbakusInntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlag grunnlag, String koblingReferanse) {
        this(grunnlag, UUID.fromString(koblingReferanse));
    }

    public AbakusInntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlag grunnlag, UUID koblingReferanse) {
        this(grunnlag.getEksternReferanse(), grunnlag.getOpprettetTidspunkt(), grunnlag);
        this.koblingReferanse = koblingReferanse;
    }

    public AbakusInntektArbeidYtelseGrunnlag(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
        super(grunnlagReferanse, opprettetTidspunkt);
    }

    public AbakusInntektArbeidYtelseGrunnlag(UUID eksternReferanse, LocalDateTime opprettetTidspunkt, InntektArbeidYtelseGrunnlag grunnlag) {
        super(eksternReferanse, opprettetTidspunkt, grunnlag);
    }

    @Override
    public Optional<UUID> getKoblingReferanse() {
        return Optional.of(this.koblingReferanse);
    }

    @Override
    public void fjernSaksbehandlet() {
        super.fjernSaksbehandlet();
    }
}
