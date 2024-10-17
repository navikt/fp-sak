package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateInterval;

/**
 * Tjenester for å bestemme tidsperiode for innhenting og lagring av behandlingsgrunnlag
 * Det er ønskelig med et fikspunkt som er stabilt gjennom behandlingen - med mindre saksbehandlervurdering/hendelser tilsier et nytt fikspunkt
 * Tjenesten må være robust og kunne gi en dato før registerinnhenting - må derfor være basert på søknadsopplysninger (på godt og vondt)
 * - Vil hente inn data 12mnd før vanlig skjæringstidspunkt (STP) og som dekker stønadsperioden (uttaket inntil 3år, 37 uker eller 1 dag)
 * - Forutgående medlemskap for ES tilsier data som dekker 12 måneder før termindato (uansett når fødsel skjer)
 * - Innhentingsperioden må tåle behandling/vedtak foregår før STP og at fødsel kan gi tidligere STP (eller for SVP kortere stønadsperiode)
 * - Bruker termin/fødsel/omsorg fra søknad for å utlede fikspunkt for sakskomplekset. Inntil videre brukes tilretteleggingsbehov for SVP.
 * - Legger på et vindu på 5 måneder for å dekke tidlig fødsel (18,5 u før termin) og mindre endringer gjort av skasbehandler
 * - Dersom saksbehandler korrigerer søknadsdata mer enn 5 måneder legges det opp til at korrigert dato brukes som fikspunkt
 */
@ApplicationScoped
public class OpplysningsPeriodeTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OpplysningsPeriodeTjeneste.class);

    private static final Map<FagsakYtelseType, Period> FØR = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, Period.ofMonths(17),
        FagsakYtelseType.FORELDREPENGER, Period.ofMonths(17),
        FagsakYtelseType.SVANGERSKAPSPENGER, Period.ofMonths(17));

    private static final Map<FagsakYtelseType, Period> ETTER = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, Period.ofMonths(6),
        FagsakYtelseType.FORELDREPENGER, Period.ofYears(4),
        FagsakYtelseType.SVANGERSKAPSPENGER, Period.ofMonths(15));

    private static final Period NÆRING_FØR = Period.ofYears(4);
    private static final Period NÆRING_ETTER_FP = Period.ofYears(3); // Stønadsperioden
    private static final Period NÆRING_ETTER_SVP = Period.ofYears(1); // Så lenge vi bruker tilretteleggingsdato. Endres til 0 ved bruk av termindato

    /**
     * Maks avvik før/etter STP for registerinnhenting før justering av perioden.
     * Basert på behov for innhenting siste 12mnd før min(behandlingsdato, stp), padding i FØR, samt fødsel fom uke 22.
     */
    private static final Period GRENSEVERDI = Period.ofMonths(5);

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private OpptjeningRepository opptjeningRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    OpplysningsPeriodeTjeneste() {
        // CDI
    }

    @Inject
    public OpplysningsPeriodeTjeneste(BehandlingRepository behandlingRepository,
                                      FamilieHendelseRepository familieGrunnlagRepository,
                                      OpptjeningRepository opptjeningRepository,
                                      SvangerskapspengerRepository svangerskapspengerRepository,
                                      YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.familieGrunnlagRepository = familieGrunnlagRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    /**
     * Beregner opplysningsperioden (Perioden vi ber om informasjon fra registerne) for en gitt behandling.
     *
     */
    public SimpleLocalDateInterval beregn(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, false);
    }

    public SimpleLocalDateInterval beregnTilOgMedIdag(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, true);
    }

    public SimpleLocalDateInterval beregnForNæringPGI(Long behandlingId, FagsakYtelseType ytelseType) {
        var fikspunkt = utledFikspunktForRegisterInnhenting(behandlingId, ytelseType);
        return switch (ytelseType) {
            case FORELDREPENGER -> SimpleLocalDateInterval.fraOgMedTomNotNull(fikspunkt.minus(NÆRING_FØR), fikspunkt.plus(NÆRING_ETTER_FP));
            case SVANGERSKAPSPENGER -> SimpleLocalDateInterval.fraOgMedTomNotNull(fikspunkt.minus(NÆRING_FØR), fikspunkt.plus(NÆRING_ETTER_SVP));
            default -> throw new IllegalArgumentException("Skal ikke innhente næring for ytelse " + ytelseType);
        };
    }

    private SimpleLocalDateInterval beregning(Long behandlingId, FagsakYtelseType ytelseType, boolean tilOgMedIdag) {
        var fikspunkt = utledFikspunktForRegisterInnhenting(behandlingId, ytelseType);
        var intervall = beregnInterval(fikspunkt.minus(FØR.get(ytelseType)), fikspunkt.plus(ETTER.get(ytelseType)), tilOgMedIdag);
        return vurderOverstyrtStartdatoForRegisterInnhenting(behandlingId, intervall);
    }

    private SimpleLocalDateInterval beregnInterval(LocalDate fom, LocalDate tom, boolean tilOgMedIdag) {
        return SimpleLocalDateInterval.fraOgMedTomNotNull(fom, tilOgMedIdag && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

    public LocalDate utledFikspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> utledFikspunktForRegisterInnhentingFraFamilieHendelse(behandlingId, FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon);
            case FORELDREPENGER -> utledFikspunktForRegisterInnhentingFraFamilieHendelse(behandlingId, fha -> Optional.of(fha.getGjeldendeVersjon()));
            case SVANGERSKAPSPENGER -> utledFikspunktRegisterinnhentingForSVP(behandlingId);
            default -> throw new IllegalStateException("Utvikler-feil: mangler ytelsetype " + ytelseType);
        };
    }
    public LocalDate utledFikspunktForRegisterInnhentingFraFamilieHendelse(Long behandlingId,
                                                                           Function<FamilieHendelseGrunnlagEntitet, Optional<FamilieHendelseEntitet>> gjeldende) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(OpplysningsPeriodeTjeneste::utledFikspunktForRegisterInnhentingFraFamilieHendelse)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: finner ikke fikspunkt for registerinnhenting behandling " + behandlingId));
    }

    public static LocalDate utledFikspunktForRegisterInnhentingFraFamilieHendelse(FamilieHendelseGrunnlagEntitet familieHendelseAggregat) {
        var oppgittHendelseDato = familieHendelseAggregat.getSøknadVersjon().getSkjæringstidspunkt();
        var gjeldendeHendelseDato = familieHendelseAggregat.getGjeldendeVersjon().getSkjæringstidspunkt();

        return erEndringIPerioden(oppgittHendelseDato, gjeldendeHendelseDato) ? gjeldendeHendelseDato : oppgittHendelseDato;
    }

    public static boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        var intervall = new LocalDateInterval(oppgittSkjæringstidspunkt.minus(GRENSEVERDI), oppgittSkjæringstidspunkt.plus(GRENSEVERDI));
        if (bekreftetSkjæringstidspunkt != null && !intervall.contains(bekreftetSkjæringstidspunkt)) {
            LOG.info("Opplysningsperiode: endring i periode foroppgitt {} bekreftet {}", oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
            return true;
        } else {
            return false;
        }
    }

    public LocalDate utledFikspunktRegisterinnhentingForSVP(Long behandlingId) {
        // Logger for å vurdere om vi skal gå over til å bruke termindato som baseline. Man bekrefter termin sammen med behovFom og tilretteleggingFom
        var familiehendelse = utledFikspunktForRegisterInnhentingFraFamilieHendelse(behandlingId, FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon);
        var tilrettelegging = utledFikspunktRegisterinnhentingFraTilretteleggingsbehov(behandlingId);
        var avstand1 = ChronoUnit.DAYS.between(tilrettelegging, familiehendelse);
        var avstand2 = ChronoUnit.DAYS.between(familiehendelse.minusWeeks(42), tilrettelegging);
        var terminIntervall = new LocalDateInterval(familiehendelse.minusWeeks(42), familiehendelse);
        if (!terminIntervall.contains(tilrettelegging)) {
            LOG.info("Opplysningsperiode: SVP ukurant avstand for behandling {} tilrettelegging {} og termin {} behov-termin {} terminf42-behov {}",
                behandlingId, tilrettelegging, familiehendelse, avstand1, avstand2);
        } else {
            LOG.info("Opplysningsperiode: SVP kurant avstand for behandling {} tilrettelegging {} og termin {} behov-termin {} terminf42-behov {}",
                behandlingId, tilrettelegging, familiehendelse, avstand1, avstand2);
        }

        return tilrettelegging;
    }

    public LocalDate utledFikspunktRegisterinnhentingFraTilretteleggingsbehov(Long behandlingId) {
        var svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .or(() -> svangerskapspengerRepository.hentGrunnlag(originalBehandling(behandlingId)));

        var tidligsteTilretteleggingsDatoOpt = svpGrunnlagOpt
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(Comparator.naturalOrder());

        var gjeldendeTilretteleggingsDatoOpt = svpGrunnlagOpt
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(Comparator.naturalOrder());

        if (tidligsteTilretteleggingsDatoOpt.isPresent() && gjeldendeTilretteleggingsDatoOpt.isPresent()) {
            var intervallBehovFom = new LocalDateInterval(tidligsteTilretteleggingsDatoOpt.get().minus(GRENSEVERDI),
                tidligsteTilretteleggingsDatoOpt.get().plus(GRENSEVERDI));
            return intervallBehovFom.contains(gjeldendeTilretteleggingsDatoOpt.get()) ?
                tidligsteTilretteleggingsDatoOpt.get() : gjeldendeTilretteleggingsDatoOpt.get();
        }
        LOG.info("Opplysningsperiode: SVP mangler tilrettelegginger, behandling {}", behandlingId);
        // Har ikke tilgjengelig data om tilrettelegginger - bør ikke skje
        return opptjeningRepository.finnOpptjening(behandlingId).map(o -> o.getTom().plusDays(1))
            .orElseGet(LocalDate::now);
    }

    private Long originalBehandling(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.erRevurdering() ? behandling.getOriginalBehandlingId().orElseThrow() : behandlingId;
    }

    public SimpleLocalDateInterval vurderOverstyrtStartdatoForRegisterInnhenting(Long behandlingId, SimpleLocalDateInterval intervall) {
        // Avklart startdato foreldrepenger er svært sent i stønadsperioden
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato)
            .filter(ovs -> intervall.getTomDato().isBefore(ovs.plusYears(1)))
            .map(ovs -> SimpleLocalDateInterval.fraOgMedTomNotNull(intervall.getFomDato(), ovs.plusYears(1)))
            .orElse(intervall);
    }
}
