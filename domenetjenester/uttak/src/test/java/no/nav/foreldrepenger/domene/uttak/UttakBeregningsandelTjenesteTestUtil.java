package no.nav.foreldrepenger.domene.uttak;

import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;

public class UttakBeregningsandelTjenesteTestUtil {

    private final Set<BeregningsgrunnlagStatus> bgStatuser = new HashSet<>();

    public Set<BeregningsgrunnlagStatus> hentStatuser() {
        return this.bgStatuser;
    }

    public UttakBeregningsandelTjenesteTestUtil leggTilSelvNæringdrivende(Arbeidsgiver arbeidsgiver) {
        this.bgStatuser.add(new BeregningsgrunnlagStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidsgiver, null));
        return this;
    }

    public UttakBeregningsandelTjenesteTestUtil leggTilFrilans() {
        this.bgStatuser.add(new BeregningsgrunnlagStatus(AktivitetStatus.FRILANSER,null, null));
        return this;
    }

    public UttakBeregningsandelTjenesteTestUtil leggTilOrdinærtArbeid(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.bgStatuser.add(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef));
        return this;
    }
}
