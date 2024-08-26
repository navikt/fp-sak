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

    /**
     * Konfig angir perioden med registerinnhenting før/etter skjæringstidspunktet (for en gitt ytelse)
     */
    @Inject
    public OpplysningsPeriodeTjeneste(BehandlingRepository behandlingRepository, FamilieHendelseRepository familieGrunnlagRepository, OpptjeningRepository opptjeningRepository, SvangerskapspengerRepository svangerskapspengerRepository,
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
     * Benytter konfig-verdier for å setter lengden på intervallene på hver side av skjæringstidspunkt for registerinnhenting.
     *
     * @param behandling behandlingen
     * @return intervallet
     */
    public SimpleLocalDateInterval beregn(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, false);
    }

    public SimpleLocalDateInterval beregnTilOgMedIdag(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, true);
    }

    private SimpleLocalDateInterval beregning(Long behandlingId, FagsakYtelseType ytelseType, boolean tilOgMedIdag) {
        var skjæringstidspunkt = utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        var intervall = beregnInterval(skjæringstidspunkt.minus(FØR.get(ytelseType)), skjæringstidspunkt.plus(ETTER.get(ytelseType)), tilOgMedIdag);
        return vurderOverstyrtStartdatoForRegisterInnhenting(behandlingId, intervall);
    }

    private SimpleLocalDateInterval beregnInterval(LocalDate fom, LocalDate tom, boolean tilOgMedIdag) {
        return SimpleLocalDateInterval.fraOgMedTomNotNull(fom, tilOgMedIdag && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> utledKjernedatoForRegisterInnhentingFraFamilieHendelse(behandlingId, FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon);
            case FORELDREPENGER -> utledKjernedatoForRegisterInnhentingFraFamilieHendelse(behandlingId, fha -> Optional.of(fha.getGjeldendeVersjon()));
            case SVANGERSKAPSPENGER -> utledSkjæringstidspunktRegisterinnhentingForSVP(behandlingId);
            default -> throw new IllegalStateException("Utvikler-feil: mangler ytelsetype " + ytelseType);
        };
    }
    public LocalDate utledKjernedatoForRegisterInnhentingFraFamilieHendelse(Long behandlingId,
                                                                            Function<FamilieHendelseGrunnlagEntitet, Optional<FamilieHendelseEntitet>> gjeldende) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(fha -> utledKjernedatoForRegisterInnhentingFraFamilieHendelse(fha, gjeldende))
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: finner ikke fikspunkt for registerinnhenting behandling " + behandlingId));
    }

    public static LocalDate utledKjernedatoForRegisterInnhentingFraFamilieHendelse(FamilieHendelseGrunnlagEntitet familieHendelseAggregat,
                                                                                   Function<FamilieHendelseGrunnlagEntitet, Optional<FamilieHendelseEntitet>> gjeldende) {
        var oppgittHendelseDato = familieHendelseAggregat.getSøknadVersjon().getSkjæringstidspunkt();
        var gjeldendeHendelseDato = gjeldende.apply(familieHendelseAggregat).map(FamilieHendelseEntitet::getSkjæringstidspunkt);

        return gjeldendeHendelseDato.filter(bs -> erEndringIPerioden(oppgittHendelseDato, bs))
            .orElse(oppgittHendelseDato);
    }

    public static boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        var intervall = new LocalDateInterval(oppgittSkjæringstidspunkt.minus(GRENSEVERDI), oppgittSkjæringstidspunkt.plus(GRENSEVERDI));
        if (bekreftetSkjæringstidspunkt != null && !intervall.contains(bekreftetSkjæringstidspunkt)) {
            LOG.info("Opplysningsperiode: endring i periode foroppgitt {} bekreftet {}",
                oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        }
        return bekreftetSkjæringstidspunkt != null && !intervall.contains(bekreftetSkjæringstidspunkt);

    }

    public LocalDate utledSkjæringstidspunktRegisterinnhentingForSVP(Long behandlingId) {
        // Logger for å vurdere om vi skal gå over til å bruke termindato som baseline. Man bekrefter termin sammen med behovFom og tilretteleggingFom
        var familiehendelse = utledKjernedatoForRegisterInnhentingFraFamilieHendelse(behandlingId, FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon);
        var tilrettelegging = utledSkjæringstidspunktRegisterinnhentingFraTilretteleggingsbehov(behandlingId);
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

    public LocalDate utledSkjæringstidspunktRegisterinnhentingFraTilretteleggingsbehov(Long behandlingId) {
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
