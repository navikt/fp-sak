package no.nav.foreldrepenger.ytelse.beregning;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public abstract class BeregnFeriepengerTjeneste {

    private JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    public BeregnFeriepengerTjeneste(BehandlingRepositoryProvider repositoryProvider, int antallDagerFeriepenger) {
        if (antallDagerFeriepenger == 0) {
            throw new IllegalStateException("Injeksjon av antallDagerFeriepenger feilet. antallDagerFeriepenger kan ikke være 0.");
        }
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.antallDagerFeriepenger = antallDagerFeriepenger;
    }

    public BeregningsresultatFeriepenger beregnFeriepenger(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {

        Optional<Behandling> annenPartsBehandling = finnAnnenPartsBehandling(behandling);
        Optional<BeregningsresultatEntitet> annenPartsBeregningsresultat = annenPartsBehandling.flatMap(beh -> {
            if (BehandlingResultatType.getAlleInnvilgetKoder().contains(getBehandlingsresultat(beh.getId()).getBehandlingResultatType())) {
                return beregningsresultatRepository.hentBeregningsresultat(beh.getId());
            }
            return Optional.empty();
        });
        Dekningsgrad gjeldendeDekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getGjeldendeDekningsgrad();

        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(behandling, beregningsresultat,
            annenPartsBeregningsresultat, gjeldendeDekningsgrad, antallDagerFeriepenger);
        String regelInput = toJson(regelModell);

        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = RegelmodellOversetter.getSporing(evaluation);

        return MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, regelModell, regelInput, sporing);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private Optional<Behandling> finnAnnenPartsBehandling(Behandling behandling) {
        Optional<Fagsak> annenFagsakOpt = finnAnnenPartsFagsak(behandling.getFagsak());
        return annenFagsakOpt.flatMap(fagsak -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()));
    }

    private Optional<Fagsak> finnAnnenPartsFagsak(Fagsak fagsak) {
        Optional<FagsakRelasjon> optionalFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        return optionalFagsakRelasjon.flatMap(fagsakRelasjon -> fagsakRelasjon.getFagsakNrEn().equals(fagsak) ? fagsakRelasjon.getFagsakNrTo() : Optional.of(fagsakRelasjon.getFagsakNrEn()));
    }

    private String toJson(BeregningsresultatFeriepengerRegelModell grunnlag) {
        JacksonJsonConfig var10000 = this.jacksonJsonConfig;
        BeregnFeriepengerFeil var10002 = BeregnFeriepengerFeil.FACTORY;
        return var10000.toJson(grunnlag, var10002::jsonMappingFeilet);
    }

    interface BeregnFeriepengerFeil extends DeklarerteFeil {
        BeregnFeriepengerFeil FACTORY = FeilFactory.create(BeregnFeriepengerFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "FP-985762", feilmelding = "JSON mapping feilet", logLevel = LogLevel.ERROR)
        Feil jsonMappingFeilet(JsonProcessingException var1);
    }
}
