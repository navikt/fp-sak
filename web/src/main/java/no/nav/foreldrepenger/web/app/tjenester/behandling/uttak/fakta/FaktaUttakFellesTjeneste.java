package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ApplicationScoped
class FaktaUttakFellesTjeneste {

    private UttakInputTjeneste uttakInputtjeneste;
    private FaktaUttakAksjonspunktUtleder utleder;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public FaktaUttakFellesTjeneste(UttakInputTjeneste uttakInputtjeneste,
                                    FaktaUttakAksjonspunktUtleder utleder,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.uttakInputtjeneste = uttakInputtjeneste;
        this.utleder = utleder;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    FaktaUttakFellesTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(List<FaktaUttakPeriodeDto> perioder, Long behandlingId) {
        var overstyrtePerioder = perioder.stream().map(FaktaUttakFellesTjeneste::map).toList();
        ytelseFordelingTjeneste.overstyrSøknadsperioder(behandlingId, overstyrtePerioder, List.of());
        oppdaterEndringsdato(overstyrtePerioder, behandlingId);
        //TODO TFP-4873 historikk, totrinn
        //TODO TFP-4873 periode mottatt dato?
        var input = uttakInputtjeneste.lagInput(behandlingId);
        var reutlededAp = utleder.utledAksjonspunkterFor(input);
        var builder = OppdateringResultat.utenTransisjon();
        reutlededAp.forEach(ap -> builder.medEkstraAksjonspunktResultat(ap, AksjonspunktStatus.OPPRETTET));
        return builder.build();
    }

    private static OppgittPeriodeEntitet map(FaktaUttakPeriodeDto dto) {
        var builder = OppgittPeriodeBuilder.ny().medPeriode(dto.fom(), dto.tom()).medPeriodeKilde(dto.periodeKilde())
            //TODO TFP-4873 smartere enn å bare sett null
            .medDokumentasjonVurdering(null)
            .medMorsAktivitet(dto.morsAktivitet())
            .medFlerbarnsdager(dto.flerbarnsdager())
            .medPeriodeType(dto.uttakPeriodeType());

        if (dto.utsettelseÅrsak() != null) {
            builder = builder.medÅrsak(dto.utsettelseÅrsak())
                .medPeriodeType(null);
        } else if (dto.overføringÅrsak() != null) {
            builder = builder.medÅrsak(dto.overføringÅrsak());
        } else if (dto.oppholdÅrsak() != null) {
            builder = builder.medÅrsak(dto.oppholdÅrsak());
        } else if (dto.samtidigUttaksprosent() != null) {
            builder = builder.medSamtidigUttaksprosent(dto.samtidigUttaksprosent())
                .medSamtidigUttak(true);
        } else if (dto.arbeidstidsprosent() != null && dto.arbeidstidsprosent().compareTo(BigDecimal.ZERO) > 0) {
            builder = builder.medArbeidsgiver(mapArbeidsgiver(dto.arbeidsforhold()))
                .medGraderingAktivitetType(mapAktivitetType(dto.arbeidsforhold()))
                .medArbeidsprosent(dto.arbeidstidsprosent());
        }
        return builder.build();
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
            var førsteDag = fomDato(perioder);
            if (førsteDag.isBefore(avklarteDatoer.get().getGjeldendeEndringsdato())) {
                var nyeAvklarteDatoer = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medJustertEndringsdato(førsteDag).build();
                var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
                    .medAvklarteDatoer(nyeAvklarteDatoer).build();
                ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
            }
        }
    }

    private LocalDate fomDato(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(p -> p.getFom()).min(Comparator.naturalOrder()).orElseThrow();
    }
}
