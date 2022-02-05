package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VERGE;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetModell;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

@BehandlingStegRef(kode = "INREG_AVSL")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(InnhentRegisteropplysningerResterendeOppgaverStegImpl.class);

    private BehandlingRepository behandlingRepository;
    private FagsakTjeneste fagsakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private KompletthetModell kompletthetModell;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private BeregningsresultatRepository brRepository;
    private PersonopplysningRepository poRepostitory;
    private FagsakRelasjonRepository fagsakRelasjonTjeneste;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepositoryProvider repositoryProvider,
            FagsakTjeneste fagsakTjeneste,
            PersonopplysningTjeneste personopplysningTjeneste,
            FamilieHendelseTjeneste familieHendelseTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            KompletthetModell kompletthetModell,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakTjeneste = fagsakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.kompletthetModell = kompletthetModell;
        this.enhetTjeneste = enhetTjeneste;
        this.brRepository = repositoryProvider.getBeregningsresultatRepository();
        this.poRepostitory = repositoryProvider.getPersonopplysningRepository();
        this.fagsakRelasjonTjeneste = repositoryProvider.getFagsakRelasjonRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        var etterlysIM = kompletthetModell.vurderKompletthet(ref, List.of(AUTO_VENT_ETTERLYST_INNTEKTSMELDING));
        if (!etterlysIM.erOppfylt()) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent).
            // Utført på/etter frist antas automatisk gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(opprettForAksjonspunktMedFrist(AUTO_VENT_ETTERLYST_INNTEKTSMELDING,
                    Venteårsak.VENT_OPDT_INNTEKTSMELDING, etterlysIM.getVentefrist())));
            }
        }

        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var barnSøktStønadFor = familieHendelseTjeneste.finnBarnSøktStønadFor(ref, personopplysninger);

        fagsakTjeneste.oppdaterFagsak(behandling, personopplysninger, barnSøktStønadFor);

        enhetTjeneste.sjekkEnhetEtterEndring(behandling)
                .ifPresent(e -> enhetTjeneste.oppdaterBehandlendeEnhet(behandling, e, HistorikkAktør.VEDTAKSLØSNINGEN, "Personopplysning"));

        try {
            sjekkPlussLoggSenereFagsaker(behandling, ref, skjæringstidspunkter);
        } catch (Exception e) {
            // NOSONAR
        }

        return erSøkerUnder18ar(ref) ? BehandleStegResultat.utførtMedAksjonspunkter(List.of(AVKLAR_VERGE)) : BehandleStegResultat.utførtUtenAksjonspunkter();

    }

    private boolean erSøkerUnder18ar(BehandlingReferanse ref) {
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var søker = personopplysninger.getSøker();
        return søker.getFødselsdato().isAfter(LocalDate.now().minusYears(18));
    }

    private void sjekkPlussLoggSenereFagsaker(Behandling behandling, BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var aktuellAntattFørstedag = stp.getUtledetSkjæringstidspunkt();
        var aktuellFamilieHendelseIntervaller = familieHendelseTjeneste.forventetFødselsIntervaller(ref);
        var egenSak = new MuligSak(behandling.getFagsakYtelseType(), behandling.getFagsak().getSaksnummer(), SaksForhold.EGEN_SAK, aktuellAntattFørstedag, aktuellFamilieHendelseIntervaller);
        var alleEgneSaker = fagsakTjeneste.finnFagsakerForAktør(ref.getAktørId()).stream()
                .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
                .filter(f -> !behandling.getFagsak().getSaksnummer().equals(f.getSaksnummer()))
                .toList();
        Set<MuligSak> egneMuligeSaker = new HashSet<>();
        alleEgneSaker.stream()
                .filter(this::fagsakHarUtbetaling)
                .filter(f -> f.getYtelseType().equals(behandling.getFagsakYtelseType()) ||
                        (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) && FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType())))
                .forEach(f -> {
                    var intervaller = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f.getId())
                            .map(b -> familieHendelseTjeneste.forventetFødselsIntervaller(b.getId())).orElse(List.of());
                    var førsteUtbetaling = fagsakHarUtbetalingFOM(f);
                    egneMuligeSaker.add(new MuligSak(f.getYtelseType(), f.getSaksnummer(), SaksForhold.EGEN_SAK, førsteUtbetaling, intervaller));
                });

        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && !RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
            Set<Long> fagsakerSomRefererer = new HashSet<>();
            fagsakerSomRefererer.addAll(poRepostitory.fagsakerMedOppgittAnnenPart(behandling.getAktørId()));
            alleEgneSaker.stream()
                    .flatMap(f -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(f.getId()).map(fr -> fr.getRelatertFagsakFraId(f.getId()).map(Fagsak::getId)).stream())
                    .flatMap(Optional::stream)
                    .forEach(fagsakerSomRefererer::add);
            fagsakerSomRefererer.stream()
                    .filter(f -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsakFraId(f)).filter(f2 -> f2.getSaksnummer().equals(egenSak.saksnummer())).isEmpty())
                    .flatMap(f -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f).stream())
                    .filter(b -> FagsakYtelseType.FORELDREPENGER.equals(b.getFagsakYtelseType()) && RelasjonsRolleType.erMor(b.getRelasjonsRolleType()))
                    .filter(this::behandlingHarUtbetaling)
                    .forEach(b -> {
                        var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(b.getId());
                        var utbetaltFom = fagsakHarUtbetalingFOM(b.getFagsak());
                        egneMuligeSaker.add(new MuligSak(b.getFagsakYtelseType(), b.getFagsak().getSaksnummer(), SaksForhold.ANNEN_PART_SAK, utbetaltFom, intervaller));
            });
        }
        var filtrert = egneMuligeSaker.stream()
                .filter(s -> s.innvilgetFom().isAfter(egenSak.innvilgetFom()))
                .toList();
        var førstUt = filtrert.stream().min(Comparator.comparing(MuligSak::innvilgetFom));
        if (!filtrert.isEmpty()) {
            LOG.info("NESTEBARN sak {} neste sak {} nyere saker {}", egenSak, førstUt, filtrert);
        }
    }

    private boolean fagsakHarUtbetaling(Fagsak f) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f.getId()).map(this::behandlingHarUtbetaling).orElse(false);
    }

    private boolean behandlingHarUtbetaling(Behandling b) {
        return brRepository.hentUtbetBeregningsresultat(b.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .anyMatch(p -> p.getDagsats() > 0);
    }

    private LocalDate fagsakHarUtbetalingFOM(Fagsak f) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f.getId()).map(this::behandlingHarUtbetalingFOM).orElse(Tid.TIDENES_BEGYNNELSE);

    }

    private LocalDate behandlingHarUtbetalingFOM(Behandling b) {
        return brRepository.hentUtbetBeregningsresultat(b.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .filter(p -> p.getDagsats() > 0)
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_BEGYNNELSE);
    }


    private static record MuligSak(FagsakYtelseType ytelse, Saksnummer saksnummer, SaksForhold relasjon, LocalDate innvilgetFom, List<LocalDateInterval> hendelseintervaller) {}

    private enum SaksForhold { EGEN_SAK, ANNEN_PART_SAK }

}
