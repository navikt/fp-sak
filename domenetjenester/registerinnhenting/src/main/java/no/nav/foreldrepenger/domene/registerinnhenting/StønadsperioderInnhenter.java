package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.vedtak.konfig.Tid;

/*
 * Finner og logger fagsaker som representerer en ny stønadsperiodeFP og dermed setter grenser for aktuell behandling.
 *
 * Anvendelse: Automatisk avkorting av stønadsperiodeFP i uttak, WLB-minsteretter. Mulig restsaldoutregning
 *
 * Regler
 * - Mor og Far/Medmor sine saker løper iht Ftl 14-10 tredje ledd - for far/medmor inntil implisert i nytt saksforhold (ny fødsel)
 * - Stønadsperiode FP begynner normalt senest 3 uker før termin eller ved omsorgsovertagelsesdato
 * - Fødsel >3uker før termin gir normalt startdato = fødselsdato
 * - Det finnes tilfeller av utsettelse av perioden som er forbeholdt mor pga sykdom, skade, innleggelse, mv.
 * - Stønadsperiode startdato = min(innvilget uttak+utsettelse eller avslag/søknadsfrist) for sakskomplekset
 *
 * Logikk:
 * - Aktuell = SVP: Finn SVP+FP for samme bruker med senere startdato
 * - Aktuell = FP/mor: Finn FP for samme bruker med senere startdato
 * - Aktuell = FP/ikkeMor: Finn FP med senere startdato - brukers egne saker + saker der bruker er oppgitt som annenpart
 * - Teller ikke saker som er kun henlagt, avslått eller opphørt uten utbetaling
 *
 * Avklaringer som pågår
 * - Relasjon til aksjonspunkt 5031 - avklar om bruker mottar annen ytelse for samme barn. Frekvens av tilfelle med flere FP-saker for samme barn?
 * - P15L 21/22: Definisjon på "tette fødsler" - fødselsdato vs utbetalingsstart
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
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;


    public StønadsperioderInnhenter() {
    }

    @Inject
    public StønadsperioderInnhenter(BehandlingRepositoryProvider repositoryProvider,
                                    FamilieHendelseTjeneste familieHendelseTjeneste,
                                    StønadsperiodeTjeneste stønadsperiodeTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.stønadsperiodeTjeneste = stønadsperiodeTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    public Optional<MuligSak> finnSenereStønadsperioderLoggResultat(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) return Optional.empty();

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        var forrigeInnvilgetFom = stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak());
        var aktuellAntattFørstedag = stp.getUtledetSkjæringstidspunkt(); // I tilfelle 1gang eller søknad om tidligere dager
        // OBS - følge med på denne for tilfelle av fedre som kan søke uttak for B1 etter at mor har fått B2
        var brukStartdato = forrigeInnvilgetFom.filter(d -> d.isBefore(aktuellAntattFørstedag)).isPresent() ?
            forrigeInnvilgetFom.get() : aktuellAntattFørstedag;
        var fhDato = finnFamilieHendelseDato(behandling);
        var egenSak = new MuligSak(behandling.getFagsakYtelseType(), behandling.getFagsak().getSaksnummer(), SaksForhold.EGEN_SAK,
            brukStartdato, fhDato.orElse(Tid.TIDENES_BEGYNNELSE));

        var alleEgneSaker = fagsakRepository.hentForBruker(behandling.getAktørId()).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> f.getOpprettetTidspunkt().isAfter(behandling.getFagsak().getOpprettetTidspunkt().minusMonths(40)))
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
            var fagsakerSomReferererId = new HashSet<>(fagsakerSomRefererer.stream().map(Fagsak::getId).toList());
            var egneFagsakerId = alleEgneSaker.stream().map(Fagsak::getId).collect(Collectors.toSet());
            alleEgneSaker.stream()
                .flatMap(f -> fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsak(f)).stream())
                .filter(f -> !fagsakerSomReferererId.contains(f.getId()))
                .forEach(f -> { fagsakerSomRefererer.add(f); fagsakerSomReferererId.add(f.getId()); });
            // Filtrer på saker som ikke er koblet med aktuell sak, saker som er foreldrepenger og mors sak, med utbetaling
            fagsakerSomRefererer.stream()
                .filter(f -> fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsak(f))
                    .filter(f2 -> egneFagsakerId.contains(f2.getId()) || f2.getSaksnummer().equals(egenSak.saksnummer())).isEmpty())
                .filter(f -> FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType()) && RelasjonsRolleType.erMor(f.getRelasjonsRolleType()))
                .flatMap(f -> opprettMuligSak(f, SaksForhold.ANNEN_PART_SAK).stream())
                .forEach(egneMuligeSaker::add);
        }

        // Forberede logging - første utgave før sjekking på fødsler for FP
        // SVP - samme barn er å vente. FP/mor - bør antagelig sjekke at senere FHdato - FP/ikkeMor - bør antagelig også sjekke FHdato senre
        // OBS: FHTjenenste erHendelseDatoRelevantForBehandling og matcherGrunnlagene ....
        var filtrert = egneMuligeSaker.stream()
            .filter(s -> !s.saksnummer().equals(egenSak.saksnummer()))
            .filter(s -> erRelevant(egenSak, s))
            .toList();
        var førstUt = filtrert.stream().min(Comparator.comparing(MuligSak::startdato));
        if (!filtrert.isEmpty()) {
            LOG.info("NESTEBARN sak {} neste sak {} nyere saker {}", egenSak, førstUt, filtrert);
        }
        return førstUt;
    }

    private boolean erRelevant(MuligSak egenSak, MuligSak muligSak) {
        // Sak med senere starttidspunkt eller tilfelle der Mor begynner Barn2 før Far begynner Barn1.
        // Men skal ikke slå til på koblet sak eller andre saker for samme barn eller saker for tidligere barn.
        // Krever at mulig sak har FamilieHendelse 12 uker etter sak det sjekkes mot - kan justers
        return muligSak.startdato().isAfter(egenSak.startdato()) || muligSak.fhdato().minusWeeks(6).isAfter(egenSak.fhdato().plusWeeks(6));
    }

    private Optional<MuligSak> opprettMuligSak(Fagsak fagsak, SaksForhold type) {
        var fhDato = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(this::finnFamilieHendelseDato);
        return stønadsperiodeTjeneste.stønadsperiodeStartdato(fagsak)
            .map(d -> new MuligSak(fagsak.getYtelseType(), fagsak.getSaksnummer(), type, d, fhDato.orElse(Tid.TIDENES_ENDE)));
    }

    private Optional<LocalDate> finnFamilieHendelseDato(Behandling behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
    }

    public static record MuligSak(FagsakYtelseType ytelse, Saksnummer saksnummer, SaksForhold relasjon, LocalDate startdato, LocalDate fhdato) {}

    public enum SaksForhold { EGEN_SAK, ANNEN_PART_SAK }

}
