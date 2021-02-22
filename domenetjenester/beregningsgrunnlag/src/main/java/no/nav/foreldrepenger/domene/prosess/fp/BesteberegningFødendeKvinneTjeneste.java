package no.nav.foreldrepenger.domene.prosess.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.prosess.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;

@ApplicationScoped
public class BesteberegningFødendeKvinneTjeneste {

    private static final Set<FamilieHendelseType> fødselHendelser = Set.of(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN);

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
        if (!erFødendeKvinneSomSøkerForeldrepenger(behandlingReferanse)) {
            return false;
        }

        Optional<OpptjeningAktiviteter> opptjeningForBeregning = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse, inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()));
        if (opptjeningForBeregning.isEmpty()) {
            return false;
        }

        return brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, opptjeningForBeregning.get());
    }

    boolean erFødendeKvinneSomSøkerForeldrepenger(BehandlingReferanse behandlingReferanse) {
        if (!gjelderForeldrepenger(behandlingReferanse)) {
            return false;
        }
        Optional<FamilieHendelseEntitet> familiehendelse = familieHendelseRepository
            .hentAggregatHvisEksisterer(behandlingReferanse.getBehandlingId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var familiehendelseType = familiehendelse.map(FamilieHendelseEntitet::getType).orElseThrow(() -> new IllegalStateException("Mangler FamilieHendelse#type for behandling: " + behandlingReferanse.getBehandlingId()));
        return erFødendeKvinne(behandlingReferanse.getRelasjonsRolleType(), familiehendelseType);
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
                                                                         OpptjeningAktiviteter opptjeningAktiviteter) {
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        Collection<Ytelse> ytelser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
            .getAktørYtelseFraRegister(behandlingReferanse.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(Collections.emptyList());
        return DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(opptjeningAktiviteter, ytelser, skjæringstidspunkt);
    }

}
