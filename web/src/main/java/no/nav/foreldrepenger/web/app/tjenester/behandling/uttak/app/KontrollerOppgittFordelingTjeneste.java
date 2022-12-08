package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.ALENEOMSORG;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.HV_OVELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_SØKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.NAV_TILTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.SYKDOM;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.UttakDokumentasjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakDokumentasjonDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjeneste;

@ApplicationScoped
public class KontrollerOppgittFordelingTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    public KontrollerOppgittFordelingTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                              BehandlingRepositoryProvider repositoryProvider,
                                              FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.førsteUttaksdatoTjeneste = førsteUttaksdatoTjeneste;
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    KontrollerOppgittFordelingTjeneste() {
        //For CDI proxy
    }

    public void bekreftOppgittePerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder, Behandling behandling) {
        valider(bekreftedePerioder, behandling);

        final List<OppgittPeriodeEntitet> overstyrtPerioder = new ArrayList<>();
        final List<PeriodeUttakDokumentasjonEntitet> dokumentasjonsperioder = new ArrayList<>();

        for (var bekreftetOppgittPeriodeDto : bekreftedePerioder) {
            var bekreftetPeriode = bekreftetOppgittPeriodeDto.getBekreftetPeriode();
            final var oppgittPeriodeBuilder = oversettPeriode(bekreftetPeriode);
            if (bekreftetPeriode.getÅrsak().isPresent()) {
                dokumentasjonsperioder.addAll(oversettDokumentasjonsperioder(bekreftetPeriode.getÅrsak().get(), bekreftetPeriode.getDokumentertePerioder()));
            }
            leggTilMottattDatoPåNyPeriode(behandling, oppgittPeriodeBuilder, bekreftetPeriode);
            overstyrtPerioder.add(oppgittPeriodeBuilder.build());
        }

        ytelseFordelingTjeneste.overstyrSøknadsperioder(behandling.getId(), overstyrtPerioder, dokumentasjonsperioder);
        oppdaterEndringsdato(bekreftedePerioder, behandling);
    }

    private void leggTilMottattDatoPåNyPeriode(Behandling behandling, OppgittPeriodeBuilder oppgittPeriodeBuilder, KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        if (bekreftetPeriode.getMottattDato() != null) {
            //Ikke ny
            return;
        }

        var mottattDato = uttaksperiodegrenseRepository.hentHvisEksisterer(behandling.getId())
            .map(Uttaksperiodegrense::getMottattDato).orElseGet(LocalDate::now);
        oppgittPeriodeBuilder.medMottattDato(mottattDato);
        oppgittPeriodeBuilder.medTidligstMottattDato(mottattDato);
    }

    private void valider(List<BekreftetOppgittPeriodeDto> bekreftedePerioder, Behandling behandling) {
        var førsteUttaksdato = førsteUttaksdatoTjeneste.finnFørsteUttaksdato(behandling);
        AvklarFaktaUttakValidator.validerOpplysninger(bekreftedePerioder, førsteUttaksdato);
    }

    private void oppdaterEndringsdato(List<BekreftetOppgittPeriodeDto> bekreftedePerioder, Behandling behandling) {
        var avklarteDatoer = ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getAvklarteDatoer();
        if (avklarteDatoer.isPresent()) {
            var førsteDagIBekreftedePerioder = førsteFomIBekreftedePerioder(bekreftedePerioder);
            if (førsteDagIBekreftedePerioder.isBefore(avklarteDatoer.get().getGjeldendeEndringsdato())) {
                var nyeAvklarteDatoer = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer)
                    .medJustertEndringsdato(førsteDagIBekreftedePerioder)
                    .build();
                var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
                    .medAvklarteDatoer(nyeAvklarteDatoer)
                    .build();
                ytelsesFordelingRepository.lagre(behandling.getId(), ytelseFordelingAggregat);
            }
        }
    }

    private LocalDate førsteFomIBekreftedePerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        return sortedByFom(bekreftedePerioder).get(0).getBekreftetPeriode().getFom();
    }

    private List<BekreftetOppgittPeriodeDto> sortedByFom(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        return bekreftedePerioder.stream()
            .sorted(Comparator.comparing(o -> o.getBekreftetPeriode().getFom()))
            .collect(Collectors.toList());
    }

    private OppgittPeriodeBuilder oversettPeriode(KontrollerFaktaPeriodeLagreDto faktaPeriodeDto) {
        Objects.requireNonNull(faktaPeriodeDto, "kontrollerFaktaPeriodeDto"); // NOSONAR $NON-NLS-1$
        final var periodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(faktaPeriodeDto.getFom(), faktaPeriodeDto.getTom())
            .medSamtidigUttak(faktaPeriodeDto.getSamtidigUttak())
            .medSamtidigUttaksprosent(faktaPeriodeDto.getSamtidigUttaksprosent())
            .medFlerbarnsdager(faktaPeriodeDto.isFlerbarnsdager());

        if (faktaPeriodeDto.getArbeidsgiver() != null) {
            periodeBuilder.medArbeidsgiver(hentArbeidsgiver(faktaPeriodeDto));
        }

        if (faktaPeriodeDto.getUttakPeriodeType() != null) {
            periodeBuilder.medPeriodeType(faktaPeriodeDto.getUttakPeriodeType());
        }
        if (faktaPeriodeDto.getArbeidstidsprosent() != null) {
            periodeBuilder.medGraderingAktivitetType(faktaPeriodeDto.getGraderingAktivitetType());
            periodeBuilder.medArbeidsprosent(faktaPeriodeDto.getArbeidstidsprosent());
        }
        if (erUtsettelse(faktaPeriodeDto)) {
            periodeBuilder.medÅrsak(faktaPeriodeDto.getUtsettelseÅrsak());
        } else if (erOverføring(faktaPeriodeDto)) {
            periodeBuilder.medÅrsak(faktaPeriodeDto.getOverføringÅrsak());
        } else if (erOpphold(faktaPeriodeDto)) {
            periodeBuilder.medÅrsak(faktaPeriodeDto.getOppholdÅrsak());
        }
        if (faktaPeriodeDto.getBegrunnelse() != null) {
            periodeBuilder.medBegrunnelse(faktaPeriodeDto.getBegrunnelse());
        }
        if (faktaPeriodeDto.getResultat() != null) {
            periodeBuilder.medVurdering(faktaPeriodeDto.getResultat());
        }

        if (faktaPeriodeDto.getMorsAktivitet() != null && !Årsak.UKJENT.getKode().equals(faktaPeriodeDto.getMorsAktivitet().getKode())) {
            periodeBuilder.medMorsAktivitet(faktaPeriodeDto.getMorsAktivitet());
        }

        periodeBuilder.medPeriodeKilde(faktaPeriodeDto.getPeriodeKilde());
        periodeBuilder.medMottattDato(faktaPeriodeDto.getMottattDato());
        periodeBuilder.medTidligstMottattDato(faktaPeriodeDto.getTidligstMottattDato());

        return periodeBuilder;
    }

    private Arbeidsgiver hentArbeidsgiver(KontrollerFaktaPeriodeLagreDto faktaPeriodeDto) {
        var arbeidsgiver = faktaPeriodeDto.getArbeidsgiver();
        if (!arbeidsgiver.erVirksomhet()) {
            return Arbeidsgiver.person(arbeidsgiver.getAktørId());
        }
        var arbeidsgiverIdentifikator = arbeidsgiver.getIdentifikator();
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    private List<PeriodeUttakDokumentasjonEntitet> oversettDokumentasjonsperioder(Årsak årsak, List<UttakDokumentasjonDto> dokumentasjonPerioder) {
        return dokumentasjonPerioder.stream()
            .map(periode -> {
                var dokumentasjonType = finnUttakDokumentasjonType(årsak);
                return new PeriodeUttakDokumentasjonEntitet(periode.getFom(), periode.getTom(), dokumentasjonType);
            })
            .collect(Collectors.toList());
    }

    private UttakDokumentasjonType finnUttakDokumentasjonType(Årsak årsak) {
        if (årsak != null) {
            if (SYKDOM.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.SYK_SØKER;
            }
            if (INSTITUSJON_BARN.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.INNLAGT_BARN;
            }
            if (INSTITUSJON_SØKER.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.INNLAGT_SØKER;
            }
            if (HV_OVELSE.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.HV_OVELSE;
            }
            if (NAV_TILTAK.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.NAV_TILTAK;
            }
            if (INSTITUSJONSOPPHOLD_ANNEN_FORELDER.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.INSTITUSJONSOPPHOLD_ANNEN_FORELDRE;
            }
            if (SYKDOM_ANNEN_FORELDER.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.SYKDOM_ANNEN_FORELDER;
            }
            if (IKKE_RETT_ANNEN_FORELDER.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.IKKE_RETT_ANNEN_FORELDER;
            }
            if (ALENEOMSORG.getKode().equals(årsak.getKode())) {
                return UttakDokumentasjonType.ALENEOMSORG_OVERFØRING;
            }
        }
        throw new IllegalStateException("Finner ikke uttakDokumentasjonType for årsak: " + årsak); //NOSONAR
    }

    private boolean erOverføring(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getOverføringÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getOverføringÅrsak().getKode());
    }

    private boolean erUtsettelse(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getUtsettelseÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getUtsettelseÅrsak().getKode());
    }

    private boolean erOpphold(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getOppholdÅrsak() != null && !Årsak.UKJENT.getKode().equals(bekreftetPeriode.getOppholdÅrsak().getKode());
    }

}
