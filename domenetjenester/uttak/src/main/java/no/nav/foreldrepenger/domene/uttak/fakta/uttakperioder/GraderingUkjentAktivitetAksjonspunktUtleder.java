package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.FRILANS;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class GraderingUkjentAktivitetAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public GraderingUkjentAktivitetAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    GraderingUkjentAktivitetAksjonspunktUtleder() {
        // For CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        return graderingPåUkjentAktivitet(input) ? List.of(AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET) : List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }

    private boolean graderingPåUkjentAktivitet(UttakInput input) {
        var yf = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(input.getBehandlingReferanse().behandlingId());
        if (yf.isEmpty()) {
            return false;
        }
        var beregningsgrunnlagStatuser = input.getBeregningsgrunnlagStatuser();
        return yf.get().getGjeldendeSøknadsperioder().getOppgittePerioder().stream()
            .filter(periode -> periode.isGradert())
            .anyMatch(periode -> gradererUkjentAktivitet(periode, beregningsgrunnlagStatuser));
    }

    private boolean gradererUkjentAktivitet(OppgittPeriodeEntitet søknadsperiode, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (søknadsperiode.getGraderingAktivitetType() == ARBEID && !beregningHarArbeidsgiver(søknadsperiode.getArbeidsgiver(), beregningsgrunnlagStatuser)) {
            return true;
        }
        if (søknadsperiode.getGraderingAktivitetType() == FRILANS && !beregningHarFrilanser(beregningsgrunnlagStatuser)) {
            return true;
        }
        return søknadsperiode.getGraderingAktivitetType() == SELVSTENDIG_NÆRINGSDRIVENDE && !beregningHarSelvstendigNæringsdrivende(beregningsgrunnlagStatuser);
    }

    private boolean beregningHarSelvstendigNæringsdrivende(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(bs -> bs.erSelvstendigNæringsdrivende());
    }

    private boolean beregningHarFrilanser(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(bs -> bs.erFrilanser());
    }

    private boolean beregningHarArbeidsgiver(Arbeidsgiver arbeidsgiver, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (arbeidsgiver == null) {
            return beregningsgrunnlagStatuser.stream().anyMatch(statusPeriode -> statusPeriode.erArbeidstaker());
        }
        return beregningsgrunnlagStatuser.stream().anyMatch(statusPeriode ->
            statusPeriode.erArbeidstaker()
                && statusPeriode.getArbeidsgiver().isPresent()
                && statusPeriode.getArbeidsgiver().get().equals(arbeidsgiver)
        );
    }
}
