package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;

@ApplicationScoped
public class BesteberegningFødendeKvinneTjeneste {

    private static final Set<FamilieHendelseType> fødselHendelser = Set.of(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN);
    public static final List<Arbeidskategori> ARBEIDSKATEGORI_DAGPENGER = List.of(Arbeidskategori.DAGPENGER, Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);

    private FamilieHendelseRepository familieHendelseRepository;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public BesteberegningFødendeKvinneTjeneste() {
        // CDI
    }

    @Inject
    public BesteberegningFødendeKvinneTjeneste(FamilieHendelseRepository familieHendelseRepository, OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse) {
        if (!gjelderForeldrepenger(behandlingReferanse)) {
            return false;
        }
        Optional<FamilieHendelseEntitet> familiehendelse = familieHendelseRepository
            .hentAggregatHvisEksisterer(behandlingReferanse.getBehandlingId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var familiehendelseType = familiehendelse.map(FamilieHendelseEntitet::getType).orElseThrow(() -> new IllegalStateException("Mangler FamilieHendelse#type for behandling: " + behandlingReferanse.getBehandlingId()));

        Optional<OpptjeningAktiviteter> opptjeningForBeregning = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse, inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));

        if (opptjeningForBeregning.isEmpty()) {
            return false;
        }

        return brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, familiehendelseType, opptjeningForBeregning.get());
    }

    private static boolean gjelderForeldrepenger(BehandlingReferanse behandlingReferanse) {
        return FagsakYtelseType.FORELDREPENGER.equals(behandlingReferanse.getFagsakYtelseType());
    }

    static boolean erFødendeKvinne(RelasjonsRolleType relasjonsRolleType, FamilieHendelseType type) {
        boolean erMoren = RelasjonsRolleType.MORA.equals(relasjonsRolleType);
        if (!erMoren) {
            return false;
        }
        return fødselHendelser.contains(type);
    }

    private boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse,
                                                                         FamilieHendelseType type,
                                                                         OpptjeningAktiviteter opptjeningAktiviteter) {
        if (!erFødendeKvinne(behandlingReferanse.getRelasjonsRolleType(), type)) {
            return false;
        }
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        return harDagpengerPåSkjæringstidspunktet(skjæringstidspunkt, opptjeningAktiviteter)
            || harSykepengerMedOvergangFraDagpenger(behandlingReferanse);
    }

    private boolean harSykepengerMedOvergangFraDagpenger(BehandlingReferanse behandlingReferanse) {
        Collection<Ytelse> ytelser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
            .getAktørYtelseFraRegister(behandlingReferanse.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(Collections.emptyList());
        return ytelser.stream().filter(y -> y.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(y -> y.getYtelseGrunnlag().isPresent())
            .filter(y -> y.getPeriode() != null && y.getPeriode().inkluderer(behandlingReferanse.getUtledetSkjæringstidspunkt()))
            .map(y -> y.getYtelseGrunnlag().get())
            .anyMatch(ytelseGrunnlag -> ytelseGrunnlag.getArbeidskategori()
                .map(ARBEIDSKATEGORI_DAGPENGER::contains).orElse(false));
    }

    private boolean harDagpengerPåSkjæringstidspunktet(LocalDate skjæringstidspunktOpptjening, OpptjeningAktiviteter opptjeningAktiviteter) {
        return opptjeningAktiviteter.getOpptjeningPerioder().stream()
            .filter(opptjeningPeriode -> opptjeningPeriode.getPeriode().getFom().isBefore(skjæringstidspunktOpptjening) &&
                (opptjeningPeriode.getPeriode().getTom() == null || !opptjeningPeriode.getPeriode().getTom().isBefore(skjæringstidspunktOpptjening)))
            .anyMatch(aktivitet -> aktivitet.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER));
    }

}
