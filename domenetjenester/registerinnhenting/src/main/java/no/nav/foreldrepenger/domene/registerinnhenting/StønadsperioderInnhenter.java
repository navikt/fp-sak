package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

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

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private NesteSakRepository nesteSakRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;


    public StønadsperioderInnhenter() {
    }

    @Inject
    public StønadsperioderInnhenter(BehandlingRepositoryProvider repositoryProvider,
                                    BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                    FamilieHendelseTjeneste familieHendelseTjeneste,
                                    StønadsperiodeTjeneste stønadsperiodeTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                    FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.nesteSakRepository = grunnlagRepositoryProvider.getNesteSakRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.stønadsperiodeTjeneste = stønadsperiodeTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    public void innhentNesteSak(Behandling behandling) {
        var muligsak= finnSenereStønadsperiode(behandling).orElse(null);
        if (muligsak != null) {
            nesteSakRepository.lagreNesteSak(behandling.getId(), muligsak.saksnummer(), muligsak.startdato(), muligsak.fhdato());
        } else {
            nesteSakRepository.fjernEventuellNesteSak(behandling.getId());
        }
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        var funnetId = nesteSakRepository.hentGrunnlag(behandlingId).map(NesteSakGrunnlagEntitet::getId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(NesteSakGrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(NesteSakGrunnlagEntitet.class));
    }

    public NesteSakGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        return nesteSakRepository.hentGrunnlagPåId(grunnlagId);
    }

    Optional<MuligSak> finnSenereStønadsperiode(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) return Optional.empty();

        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        var forrigeInnvilgetFom = stønadsperiodeTjeneste.stønadsperiodeStartdato(behandling.getFagsak());
        var aktuellAntattFørstedag = stp.getUtledetSkjæringstidspunkt(); // I tilfelle 1gang eller søknad om tidligere dager
        // OBS - følge med på denne for tilfelle av fedre som kan søke uttak for B1 etter at mor har fått B2
        var brukStartdato = forrigeInnvilgetFom.filter(d -> d.isBefore(aktuellAntattFørstedag)).isPresent() ?
            forrigeInnvilgetFom.get() : aktuellAntattFørstedag;
        var fhDato = finnFamilieHendelseDato(behandling);
        var egenSak = new MuligSak(behandling.getFagsakYtelseType(), behandling.getSaksnummer(),
            SaksForhold.EGEN_SAK, brukStartdato, fhDato.orElse(null));

        var egneMuligeSaker = utledEgneMuligeSaker(behandling, egenSak);

        var tidligste = egneMuligeSaker.stream()
            .filter(s -> !s.saksnummer().equals(egenSak.saksnummer()))
            .filter(s -> erRelevant(egenSak, s))
            .min(Comparator.comparing(MuligSak::startdato));
        if (tidligste.isPresent()) {
            LOG.info("NESTEBARN sak {} neste sak {}", egenSak, tidligste);
        }
        return tidligste;
    }

    private boolean erRelevant(MuligSak egenSak, MuligSak muligSak) {
        // Sak med senere starttidspunkt eller tilfelle der Mor begynner Barn2 før Far begynner Barn1.
        // Men skal ikke slå til på koblet sak eller andre saker for samme barn eller saker for tidligere barn.
        // Krever at mulig sak har FamilieHendelse 12 uker etter sak det sjekkes mot - kan justeres
        return muligSak.startdato().isAfter(egenSak.startdato()) || egenSak.fhdato() != null && muligSak.fhdato() != null && muligSak.fhdato()
            .minusWeeks(6)
            .isAfter(egenSak.fhdato().plusWeeks(6));
    }

    private Set<MuligSak> utledEgneMuligeSaker(Behandling behandling, MuligSak egenSak) {
        var alleEgneSaker = getAlleEgneSaker(behandling, egenSak);
        var aktuellType = behandling.getFagsakYtelseType();
        // SVP->SVP + FP->FP + SVP->FP. Ser ikke på FP->SVP ettersom det ikke opphører rett til FP forrige barn. Skal være sak med vedtak
        Set<MuligSak> egneMuligeSaker = new HashSet<>();
        alleEgneSaker.stream()
            .filter(f -> f.getYtelseType().equals(aktuellType)
                || FagsakYtelseType.SVANGERSKAPSPENGER.equals(aktuellType) && FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType()))
            .filter(this::harInnvilgetVedtak)
            .flatMap(f -> opprettMuligSak(behandling, f, SaksForhold.EGEN_SAK).stream())
            .forEach(egneMuligeSaker::add);
        // Finn mødres saker der bruker er implisert
        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && !RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
            leggTilMuligeSakerForAnnenPart(behandling, egenSak, alleEgneSaker, egneMuligeSaker);
        }
        return egneMuligeSaker;
    }

    private void leggTilMuligeSakerForAnnenPart(Behandling behandling, MuligSak egenSak, List<Fagsak> alleEgneSaker, Set<MuligSak> egneMuligeSaker) {
        // Populer med saker der bruker er oppgitt annen part eller som er koblet med brukers saker
        var fagsakerSomRefererer = new ArrayList<>(personopplysningRepository.fagsakerMedOppgittAnnenPart(behandling.getAktørId()));
        var fagsakerSomReferererId = new HashSet<>(fagsakerSomRefererer.stream().map(Fagsak::getId).toList());
        // Legg til eventuelle tilfelle der saker er koblet uten at det er oppgitt annen part
        for (var f : alleEgneSaker) {
            fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsak(f))
                .filter(anpa -> !fagsakerSomReferererId.contains(anpa.getId()))
                .ifPresent(anpa -> { fagsakerSomRefererer.add(anpa); fagsakerSomReferererId.add(anpa.getId()); });
        }
        // Filtrer på saker som ikke er koblet med aktuell sak, saker som er foreldrepenger og mors sak, saker med vedtak + uttak/utbetaling
        fagsakerSomRefererer.stream()
            .filter(f -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(f).flatMap(fr -> fr.getRelatertFagsak(f))
                .filter(f2 -> f2.getSaksnummer().equals(egenSak.saksnummer())).isEmpty())
            .filter(f -> FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType()) && RelasjonsRolleType.erMor(f.getRelasjonsRolleType()))
            .filter(this::harInnvilgetVedtak)
            .flatMap(f -> opprettMuligSak(behandling, f, SaksForhold.ANNEN_PART_SAK).stream())
            .forEach(egneMuligeSaker::add);
    }

    private List<Fagsak> getAlleEgneSaker(Behandling behandling, MuligSak egenSak) {
        return fagsakRepository.hentForBruker(behandling.getAktørId()).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> f.getOpprettetTidspunkt().isAfter(behandling.getFagsak().getOpprettetTidspunkt().minusMonths(40)))
            .filter(f -> !egenSak.saksnummer().equals(f.getSaksnummer()))
            .toList();
    }

    private Optional<MuligSak> opprettMuligSak(Behandling behandling, Fagsak fagsak, SaksForhold type) {
        var fhDato = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(this::finnFamilieHendelseDato);
        // Dersom aktuell sak er svp så ser vi på mors fp/svp-sak alene eller så ser vi på stønadsperioden
        var startDato = FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) ?
            stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(fagsak).map(LocalDateInterval::getFomDato) :
            stønadsperiodeTjeneste.stønadsperiodeStartdato(fagsak);

        return  startDato.map(d -> new MuligSak(fagsak.getYtelseType(), fagsak.getSaksnummer(), type, d, fhDato.orElse(null)));
    }

    private Optional<LocalDate> finnFamilieHendelseDato(Behandling behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
    }

    private boolean harInnvilgetVedtak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> behandlingVedtakRepository.hentForBehandlingHvisEksisterer(b.getId()))
            .filter(v -> !VedtakResultatType.AVSLAG.equals(v.getVedtakResultatType()))
            .isPresent();
    }

    public record MuligSak(FagsakYtelseType ytelse, Saksnummer saksnummer, SaksForhold relasjon, LocalDate startdato, LocalDate fhdato) {}

    public enum SaksForhold { EGEN_SAK, ANNEN_PART_SAK }

}
