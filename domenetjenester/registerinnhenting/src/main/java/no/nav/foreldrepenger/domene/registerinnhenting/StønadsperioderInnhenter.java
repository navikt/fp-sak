package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

/*
 * Finner og logger fagsaker som representerer en ny stønadsperiode og dermed setter grenser for aktuell behandling.
 *
 * Anvendelse: Automatisk avkorting av stønadsperiode i uttak, WLB-minsteretter. Mulig restsaldoutregning
 *
 * Regler
 * - Mor og Far/Medmor sine saker løper iht Ftl 14-10 tredje ledd - for far/medmor inntil implisert i nytt saksforhold (ny fødsel)
 * - Stønadsperiode begynner normalt senest 3 uker før termin eller ved omsorgsovertagelsesdato
 * - Fødsel >3uker før termin gir normalt startdato = fødselsdato
 * - Det finnes tilfeller av utsettelse av perioden som er forbeholdt mor pga sykdom, skade, innleggelse, mv.
 * - Stønadsperiode startdato = min(familiehendelsedato for sak med innvilget stønad, første utbetalingsdato)
 *
 * Logikk:
 * - Aktuell = SVP: Finn SVP+FP for samme bruker med senere startdato
 * - Aktuell = FP/mor: Finn FP for samme bruker med senere startdato
 * - Aktuell = FP/ikkeMor: Finn FP med senere startdato - brukers egne saker + saker der bruker er oppgitt som annenpart
 * - Teller ikke saker som er kun henlagt, avslått eller opphørt uten utbetaling
 *
 * Avklaringer som pågår
 * - Etablere prinsippene
 * - Relasjon til aksjonspunkt 5031 - avklar om bruker mottar annen ytelse for samme barn
 * - Ftl 14-14: Når BFHR begynner ny sak?
 * - P15L21/22: Definisjon på "tette fødsler" - fødselsdato vs utbetalingsstart
 * - Stønadsperiodestart for adopsjon og omsorgsovertagelse som begynner med utsettelse eller der far begynner
 * - Stønadsperiodestart for fedre/medmødre ifm sammenhengende uttak
 * - Lagringsbehov utover saksnummer og startdato - hvem, fødsel vs uttaksdato?
 *
 * OBS: Mulig denne bør flyttes nærmere uttak ettersom hverken familiehendelsedato eller stp er bekreftet så tidlig i prosessen.
 */
@ApplicationScoped
public class StønadsperioderInnhenter {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsperioderInnhenter.class);

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentRepository; // Funder på IAY-tjeneste - alle ytelser er lagret i IAYGrunnlag
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;


    public StønadsperioderInnhenter() {
    }

    @Inject
    public StønadsperioderInnhenter(BehandlingRepositoryProvider repositoryProvider,
                                    FamilieHendelseTjeneste familieHendelseTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.tilkjentRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public Optional<MuligSak> finnSenereStønadsperioderLoggResultat(Behandling behandling) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        var aktuellAntattFørstedag = stp.getUtledetSkjæringstidspunkt(); // I tilfelle 1gang der det ikke foreligger fødsel eller innvilget stønad.
        var aktuellFamilieHendelseDato = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(Tid.TIDENES_ENDE);
        // OBS - følge med på denne for tilfelle av fedre som kan søke uttak for B1 etter at mor har fått B2
        var brukStartdato = aktuellAntattFørstedag.isBefore(aktuellFamilieHendelseDato) ? aktuellAntattFørstedag : aktuellFamilieHendelseDato;
        var aktuellFamilieHendelseIntervaller = familieHendelseTjeneste.forventetFødselsIntervaller(behandling.getId());
        var egenSak = new MuligSak(behandling.getFagsakYtelseType(), behandling.getFagsak().getSaksnummer(), SaksForhold.EGEN_SAK,
                                    brukStartdato, aktuellAntattFørstedag, aktuellFamilieHendelseDato, aktuellFamilieHendelseIntervaller);

        var alleEgneSaker = fagsakRepository.hentForBruker(behandling.getAktørId()).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> !egenSak.saksnummer().equals(f.getSaksnummer()))
            .toList();
        Set<MuligSak> egneMuligeSaker = new HashSet<>();
        alleEgneSaker.stream()
            .filter(f -> f.getYtelseType().equals(behandling.getFagsakYtelseType()) ||
                         (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) && FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType())))
            .flatMap(f -> opprettMuligSak(f, SaksForhold.EGEN_SAK).stream())
            .forEach(egneMuligeSaker::add);

        // Finn mødres saker der bruker er implisert
        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && !RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
            // Populer med saker der bruker er oppgitt annen part eller som er koblet med brukers saker
            var fagsakerSomRefererer = new ArrayList<>(personopplysningRepository.fagsakerMedOppgittAnnenPart(behandling.getAktørId()));
            var fagsakerSomReferererId = fagsakerSomRefererer.stream().map(Fagsak::getId).collect(Collectors.toSet());
            alleEgneSaker.stream()
                .flatMap(f -> fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(f.getId())
                                .flatMap(fr -> fr.getRelatertFagsakFraId(f.getId())).stream())
                .filter(f -> !fagsakerSomReferererId.contains(f.getId()))
                .forEach(fagsakerSomRefererer::add);
            // Filtrer på saker som ikke er koblet med aktuell sak, saker som er foreldrepenger og mors sak, med utbetaling
            fagsakerSomRefererer.stream()
                .filter(f -> fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsak(f))
                    .filter(f2 -> f2.getSaksnummer().equals(egenSak.saksnummer())).isEmpty())
                .filter(f -> FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType()) && RelasjonsRolleType.erMor(f.getRelasjonsRolleType()))
                .flatMap(f -> opprettMuligSak(f, SaksForhold.ANNEN_PART_SAK).stream())
                .forEach(egneMuligeSaker::add);
        }

        // Forberede logging - første utgave før sjekking på fødsler for FP
        // SVP - samme barn er å vente. FP/mor - bør antagelig sjekke at senere FHdato - FP/ikkeMor - bør antagelig også sjekke FHdato senre
        // OBS: FHTjenenste erHendelseDatoRelevantForBehandling og matcherGrunnlagene ....
        var filtrert = egneMuligeSaker.stream()
            .filter(s -> !s.saksnummer().equals(egenSak.saksnummer()))
            .filter(s -> s.valgtStartdato().isAfter(egenSak.valgtStartdato()))
            .toList();
        var førstUt = filtrert.stream().min(Comparator.comparing(MuligSak::innvilgetFom));
        if (!filtrert.isEmpty()) {
            LOG.info("NESTEBARN sak {} neste sak {} nyere saker {}", egenSak, førstUt, filtrert);
        }
        return førstUt;
    }

    private Optional<MuligSak> opprettMuligSak(Fagsak fagsak, SaksForhold type) {
        // Se etter FHdato i sisteBehandling og sisteAvsluttet - Utbetaling fra sisteAvsluttet
        var sisteBehandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId()).orElse(null);
        var sisteAvsluttet = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteAvsluttet == null || sisteBehandling == null || !behandlingHarUtbetaling(sisteAvsluttet)) {
            return Optional.empty();
        }
        var sisteFHDato = familieHendelseTjeneste.finnAggregat(sisteBehandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(Tid.TIDENES_ENDE);
        var avsluttetFHDato =familieHendelseTjeneste.finnAggregat(sisteAvsluttet.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(Tid.TIDENES_ENDE);
        if (!sisteFHDato.equals(avsluttetFHDato)) {
            LOG.info("NESTEBARN ulike fh-datoer sak {} avsluttet {} siste {}", fagsak.getSaksnummer(), avsluttetFHDato, sisteFHDato);
        }
            // Se etter FHdato i siste og sisteAvsluttede
        var utbetalingFom = behandlingHarUtbetalingFOM(sisteAvsluttet).orElse(Tid.TIDENES_ENDE);
        var brukStartdato = utbetalingFom.isBefore(sisteFHDato) ? utbetalingFom : sisteFHDato;
        if (brukStartdato.equals(Tid.TIDENES_ENDE)) {
            return Optional.empty();
        } else {
            var intervaller = familieHendelseTjeneste.forventetFødselsIntervaller(sisteBehandling.getId());
            var muligSak = new MuligSak(fagsak.getYtelseType(), fagsak.getSaksnummer(), type,
                                        brukStartdato, utbetalingFom, sisteFHDato, intervaller);
            return Optional.of(muligSak);
        }
    }

    private boolean behandlingHarUtbetaling(Behandling b) {
        return tilkjentRepository.hentUtbetBeregningsresultat(b.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .anyMatch(p -> p.getDagsats() > 0);
    }

    private Optional<LocalDate> behandlingHarUtbetalingFOM(Behandling b) {
        return tilkjentRepository.hentUtbetBeregningsresultat(b.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
    }


    public static record MuligSak(FagsakYtelseType ytelse, Saksnummer saksnummer, SaksForhold relasjon,
                                   LocalDate valgtStartdato, LocalDate innvilgetFom, LocalDate hendelsedato,
                                   List<LocalDateInterval> hendelseintervaller) {}

    public enum SaksForhold { EGEN_SAK, ANNEN_PART_SAK }

}
