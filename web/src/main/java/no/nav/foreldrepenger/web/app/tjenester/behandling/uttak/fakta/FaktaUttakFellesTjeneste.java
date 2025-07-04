package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.fakta.uttak.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.TidligstMottattOppdaterer;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelseFordelingDtoTjeneste;

@ApplicationScoped
class FaktaUttakFellesTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FaktaUttakFellesTjeneste.class);

    private UttakInputTjeneste uttakInputtjeneste;
    private FaktaUttakAksjonspunktUtleder utleder;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private YtelseFordelingDtoTjeneste ytelseFordelingDtoTjeneste;
    private FaktaUttakHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private FaktaUttakPeriodeDtoTjeneste dtoTjeneste;

    @Inject
    public FaktaUttakFellesTjeneste(UttakInputTjeneste uttakInputtjeneste,
                                    FaktaUttakAksjonspunktUtleder utleder,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    YtelsesFordelingRepository ytelsesFordelingRepository,
                                    FpUttakRepository fpUttakRepository,
                                    UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                    YtelseFordelingDtoTjeneste ytelseFordelingDtoTjeneste,
                                    FaktaUttakHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    FaktaUttakPeriodeDtoTjeneste dtoTjeneste) {
        this.uttakInputtjeneste = uttakInputtjeneste;
        this.utleder = utleder;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.ytelseFordelingDtoTjeneste = ytelseFordelingDtoTjeneste;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fpUttakRepository = fpUttakRepository;
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.dtoTjeneste = dtoTjeneste;
    }

    FaktaUttakFellesTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(String begrunnelse, List<FaktaUttakPeriodeDto> perioder, Long behandlingId, Long fagsakId, boolean overstyring) {
        var gjeldendeDtos = dtoTjeneste.lagDtos(behandlingId);
        //relevanteOppgittPerioder og perioder som lagres kan inneholder vedtaksperioder fra forrige behandling
        var relevanteOppgittPerioder = dtoTjeneste.hentRelevanteOppgittPerioder(behandlingId).toList();
        var førsteGjeldendeDag = førsteUttaksdagIBehandling(behandlingId);

        var overstyrtePerioder = finnPerioderFraFørsteEndring(gjeldendeDtos, perioder, førsteGjeldendeDag)
            .stream()
            .map(p -> map(p, relevanteOppgittPerioder))
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .toList();

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        validerFørsteUttaksdag(overstyrtePerioder, behandling);
        var overstyrtePerioderMedMottattDato = oppdaterMedMottattdato(behandling, overstyrtePerioder);
        ytelseFordelingTjeneste.overstyrSøknadsperioder(behandlingId, overstyrtePerioderMedMottattDato);
        oppdaterEndringsdato(overstyrtePerioderMedMottattDato, behandlingId);
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandlingId, fagsakId, relevanteOppgittPerioder, overstyrtePerioderMedMottattDato, overstyring, begrunnelse);

        validerReutledetAksjonspunkt(behandlingId);
        return OppdateringResultat.utenTransisjon().build();
    }

    private LocalDate førsteUttaksdagIBehandling(Long behandlingId) {
        return hentGjeldendeFordeling(behandlingId)
            .stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.MIN);
    }

    private List<OppgittPeriodeEntitet> hentGjeldendeFordeling(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregat(behandlingId).getGjeldendeFordeling().getPerioder();
    }

    private List<FaktaUttakPeriodeDto> finnPerioderFraFørsteEndring(List<FaktaUttakPeriodeDto> gjeldendePerioder,
                                                                    List<FaktaUttakPeriodeDto> overstyrtePerioder,
                                                                    LocalDate førsteGjeldendeDag) {
        var funnetEndring = false;
        var perioderFraEndring = new ArrayList<FaktaUttakPeriodeDto>();
        var sorted = overstyrtePerioder.stream().sorted(Comparator.comparing(FaktaUttakPeriodeDto::fom)).toList();
        for (var overstyrt : sorted) {
            if (funnetEndring || !overstyrt.fom().isBefore(førsteGjeldendeDag) || erEndret(overstyrt, gjeldendePerioder)) {
                perioderFraEndring.add(overstyrt);
                funnetEndring = true;
            }
        }
        return perioderFraEndring;
    }

    private boolean erEndret(FaktaUttakPeriodeDto p, List<FaktaUttakPeriodeDto> gjeldendePerioder) {
        return gjeldendePerioder.stream().noneMatch(p::equals);
    }

    private void validerReutledetAksjonspunkt(Long behandlingId) {
        //Burde ikke kunne lagre et uttak som fører til AP
        var input = uttakInputtjeneste.lagInput(behandlingId);
        var perioder = hentGjeldendeFordeling(behandlingId);
        var reutlededAp = utleder.utledAksjonspunkterFor(input, perioder);
        if (!reutlededAp.stream().filter(ap -> ap != AksjonspunktDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART).toList().isEmpty()) {
            LOG.info("Lagrede perioder fører til at aksjonspunkt reutledes {}", reutlededAp);
            throw new FaktaOmUttakReutledetAksjonspunkt();
        }
    }

    private void validerFørsteUttaksdag(List<OppgittPeriodeEntitet> overstyrtePerioder, Behandling behandling) {
        //Burde overstyre første uttaksdag i Fakta om saken
        var førsteOverstyrt = overstyrtePerioder.stream().filter(p -> !p.isUtsettelse()).map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder());
        if (førsteOverstyrt.isPresent()) {
            var førsteUttaksdato = ytelseFordelingDtoTjeneste.finnFørsteUttaksdato(behandling);
            if (førsteOverstyrt.get().isBefore(førsteUttaksdato)) {
                throw new IllegalArgumentException(
                    "første dag i overstyrte perioder kan ikke ligge før gyldig første uttaksdato " + førsteOverstyrt + " - " + førsteUttaksdato);
            }
        }
    }

    private List<OppgittPeriodeEntitet> oppdaterMedMottattdato(Behandling behandling, List<OppgittPeriodeEntitet> overstyrt) {
        var gjeldendeFordelingAsList = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getGjeldendeFordeling).stream().toList();
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(ob -> fpUttakRepository.hentUttakResultatHvisEksisterer(ob));
        var ansesMottattDato = uttaksperiodegrenseRepository.hentHvisEksisterer(behandling.getId())
            .map(Uttaksperiodegrense::getMottattDato).orElseGet(LocalDate::now);
        return TidligstMottattOppdaterer.oppdaterTidligstMottattDato(overstyrt, ansesMottattDato, gjeldendeFordelingAsList, forrigeUttak);
    }

    private static OppgittPeriodeEntitet map(FaktaUttakPeriodeDto dto, List<OppgittPeriodeEntitet> gjeldende) {
        var periodeIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(dto.fom(), dto.tom());
        var gjeldendeSomOmslutter = gjeldendeSomOmslutter(periodeIntervall, gjeldende);
        var periodeKilde = dto.periodeKilde() == null ? FordelingPeriodeKilde.SAKSBEHANDLER : dto.periodeKilde();
        //legacy begrunnelse fjernes hvis saksbehandler endrer noe
        var begrunnelse = periodeKilde == FordelingPeriodeKilde.SAKSBEHANDLER || gjeldendeSomOmslutter.isEmpty() ? null : gjeldendeSomOmslutter.get()
            .getBegrunnelse()
            .orElse(null);
        var dokumentasjonVurdering = periodeKilde == FordelingPeriodeKilde.SAKSBEHANDLER ? null : gjeldendeSomOmslutter.map(
            OppgittPeriodeEntitet::getDokumentasjonVurdering).orElse(null);
        var builder = OppgittPeriodeBuilder.ny().medPeriode(dto.fom(), dto.tom())
            .medPeriodeKilde(periodeKilde)
            .medMottattDato(gjeldendeSomOmslutter.map(OppgittPeriodeEntitet::getMottattDato).orElseGet(LocalDate::now))
            .medTidligstMottattDato(gjeldendeSomOmslutter.flatMap(OppgittPeriodeEntitet::getTidligstMottattDato).orElse(null))
            .medDokumentasjonVurdering(dokumentasjonVurdering)
            .medBegrunnelse(begrunnelse)
            .medMorsAktivitet(dto.morsAktivitet())
            .medFlerbarnsdager(dto.flerbarnsdager())
            .medPeriodeType(dto.uttakPeriodeType())
            .medSamtidigUttaksprosent(dto.samtidigUttaksprosent())
            .medSamtidigUttak(dto.samtidigUttaksprosent() != null);

        if (dto.utsettelseÅrsak() != null) {
            builder = builder.medÅrsak(dto.utsettelseÅrsak())
                .medPeriodeType(null);
        } else if (dto.overføringÅrsak() != null) {
            builder = builder.medÅrsak(dto.overføringÅrsak());
        } else if (dto.oppholdÅrsak() != null) {
            builder = builder.medÅrsak(dto.oppholdÅrsak());
            builder = builder.medPeriodeType(UttakPeriodeType.UDEFINERT);
        } else if (erGradering(dto)) {
            builder = builder.medArbeidsgiver(mapArbeidsgiver(dto.arbeidsforhold()))
                .medGraderingAktivitetType(mapAktivitetType(dto.arbeidsforhold()))
                .medArbeidsprosent(dto.arbeidstidsprosent());
        }
        return builder.build();
    }

    private static boolean erGradering(FaktaUttakPeriodeDto dto) {
        return dto.arbeidstidsprosent() != null && dto.arbeidstidsprosent().compareTo(BigDecimal.ZERO) > 0;
    }

    private static GraderingAktivitetType mapAktivitetType(ArbeidsforholdDto arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return switch (arbeidsforhold.arbeidType()) {
            case ORDINÆRT_ARBEID -> GraderingAktivitetType.ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> GraderingAktivitetType.FRILANS;
            case ANNET -> null;
        };
    }

    private static Arbeidsgiver mapArbeidsgiver(ArbeidsforholdDto arbeidsforhold) {
        if (arbeidsforhold == null || arbeidsforhold.arbeidsgiverReferanse() == null) {
            return null;
        }
        if (OrgNummer.erGyldigOrgnr(arbeidsforhold.arbeidsgiverReferanse())) {
            return Arbeidsgiver.virksomhet(arbeidsforhold.arbeidsgiverReferanse());
        }
        return Arbeidsgiver.person(new AktørId(arbeidsforhold.arbeidsgiverReferanse()));
    }

    private void oppdaterEndringsdato(List<OppgittPeriodeEntitet> perioder, Long behandlingId) {
        var avklarteDatoer = ytelseFordelingTjeneste.hentAggregat(behandlingId).getAvklarteDatoer();
        if (avklarteDatoer.isPresent()) {
            var førsteDag = perioder.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder()).orElseThrow();
            if (førsteDag.isBefore(avklarteDatoer.get().getGjeldendeEndringsdato())) {
                var nyeAvklarteDatoer = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medJustertEndringsdato(førsteDag).build();
                var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
                    .medAvklarteDatoer(nyeAvklarteDatoer).build();
                ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
            }
        }
    }

    private static Optional<OppgittPeriodeEntitet> gjeldendeSomOmslutter(DatoIntervallEntitet intervall, List<OppgittPeriodeEntitet> gjeldende) {
        return gjeldende.stream()
            .filter(p -> intervall.erOmsluttetAv(p.getTidsperiode()))
            .findFirst();
    }


}
