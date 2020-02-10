package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIUttakFraEndringsdato;
import no.nav.foreldrepenger.behandling.revurdering.felles.ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFellesImpl;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.EndringsdatoRevurderingUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@Dependent
@FagsakYtelseTypeRef("SVP")
@BehandlingTypeRef("BT-004")
public class RevurderingBehandlingsresultatutleder extends RevurderingBehandlingsresultatutlederFellesImpl {

    private SvangerskapspengerUttakResultatRepository uttakRepository;
    private EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public RevurderingBehandlingsresultatutleder(BehandlingRepositoryProvider repositoryProvider, // NOSONAR
                                                    HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    @FagsakYtelseTypeRef("SVP") EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder,
                                                    RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                                    OpphørUttakTjeneste opphørUttakTjeneste,
                                                    UttakInputTjeneste uttakInputTjeneste,
                                                    @FagsakYtelseTypeRef("SVP") HarEtablertYtelse harEtablertYtelse,
                                                    @FagsakYtelseTypeRef("SVP") ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato,
                                                    @FagsakYtelseTypeRef("SVP") ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                    @FagsakYtelseTypeRef("SVP") SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    MedlemTjeneste medlemTjeneste) {
        super(repositoryProvider,
            beregningsgrunnlagTjeneste,
            medlemTjeneste,
            opphørUttakTjeneste,
            harEtablertYtelse,
            erEndringIUttakFraEndringsdato,
            erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
            skjæringstidspunktTjeneste
        );
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    @Override
    protected UttakResultatHolder getUttakResultat(Long behandlingId) {
        return new UttakResultatHolderImpl(uttakRepository.hentHvisEksisterer(behandlingId));
    }

    @Override
    protected boolean erSluttPåStønadsdager(Behandling behandling) {
        return false;
    }

    @Override
    protected UttakResultatHolder getAnnenPartUttak(Saksnummer saksnummer) {
        return new UttakResultatHolderImpl(relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattUttaksplanSVP(saksnummer)
            .filter(this::erTilknyttetLøpendeFagsak));
    }

    @Override
    protected LocalDate finnEndringsdato(BehandlingReferanse ref) {
        return endringsdatoRevurderingUtleder.utledEndringsdato(uttakInputTjeneste.lagInput(ref.getBehandlingId()));
    }

    private boolean erTilknyttetLøpendeFagsak(SvangerskapspengerUttakResultatEntitet uttakResultatEntitet) {
        return uttakResultatEntitet.getBehandlingsresultat().getBehandling().getFagsak().getStatus().equals(FagsakStatus.LØPENDE);
    }
}
