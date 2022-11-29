package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.fakta.v2.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.TidligstMottattOppdaterer;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjeneste;

@ApplicationScoped
class FaktaUttakFellesTjeneste {

    private UttakInputTjeneste uttakInputtjeneste;
    private FaktaUttakAksjonspunktUtleder utleder;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste;
    private BehandlingRepository behandlingRepository;
    private FpUttakRepository fpUttakRepository;

    @Inject
    public FaktaUttakFellesTjeneste(UttakInputTjeneste uttakInputtjeneste,
                                    FaktaUttakAksjonspunktUtleder utleder,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    YtelsesFordelingRepository ytelsesFordelingRepository,
                                    FpUttakRepository fpUttakRepository,
                                    FørsteUttaksdatoTjeneste førsteUttaksdatoTjeneste,
                                    BehandlingRepository behandlingRepository) {
        this.uttakInputtjeneste = uttakInputtjeneste;
        this.utleder = utleder;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.førsteUttaksdatoTjeneste = førsteUttaksdatoTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fpUttakRepository = fpUttakRepository;
    }

    FaktaUttakFellesTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(List<FaktaUttakPeriodeDto> perioder, Long behandlingId) {
        var overstyrtePerioder = perioder.stream().map(FaktaUttakFellesTjeneste::map).toList();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        validerFørsteUttaksdag(overstyrtePerioder, behandling);
        var overstyrtePerioderMedMottattDato = oppdaterMedMottattdato(behandling, overstyrtePerioder);
        ytelseFordelingTjeneste.overstyrSøknadsperioder(behandlingId, overstyrtePerioderMedMottattDato, List.of());
        oppdaterEndringsdato(overstyrtePerioderMedMottattDato, behandlingId);
        //TODO TFP-4873 historikk, totrinn
        //TODO TFP-4873 periode mottatt dato?
        validerReutledetAksjonspunkt(behandlingId);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void validerReutledetAksjonspunkt(Long behandlingId) {
        //Burde ikke kunne lagre et uttak som fører til AP
        var input = uttakInputtjeneste.lagInput(behandlingId);
        var reutlededAp = utleder.utledAksjonspunkterFor(input);
        if (!reutlededAp.isEmpty()) {
            throw new IllegalStateException("Lagrede perioder fører til at aksjonspunkt reutledes");
        }
    }

    private void validerFørsteUttaksdag(List<OppgittPeriodeEntitet> overstyrtePerioder, Behandling behandling) {
        //Burde overstyre første uttaksdag i Fakta om saken
        var førsteOverstyrt = overstyrtePerioder.stream().filter(p -> !p.isUtsettelse()).map(p -> p.getFom()).min(Comparator.naturalOrder());
        if (førsteOverstyrt.isPresent()) {
            var førsteUttaksdato = førsteUttaksdatoTjeneste.finnFørsteUttaksdato(behandling).orElseThrow();
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
        return TidligstMottattOppdaterer.oppdaterTidligstMottattDato(overstyrt, LocalDate.now(), gjeldendeFordelingAsList, forrigeUttak);

    }

    private static OppgittPeriodeEntitet map(FaktaUttakPeriodeDto dto) {
        var builder = OppgittPeriodeBuilder.ny().medPeriode(dto.fom(), dto.tom()).medPeriodeKilde(dto.periodeKilde())
            //TODO TFP-4873 smartere enn å bare sett null
            .medDokumentasjonVurdering(null)
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
            var førsteDag = perioder.stream().map(p -> p.getFom()).min(Comparator.naturalOrder()).orElseThrow();
            if (førsteDag.isBefore(avklarteDatoer.get().getGjeldendeEndringsdato())) {
                var nyeAvklarteDatoer = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer).medJustertEndringsdato(førsteDag).build();
                var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
                    .medAvklarteDatoer(nyeAvklarteDatoer).build();
                ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
            }
        }
    }

}
