package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

@ApplicationScoped
class InntektsmeldingDtoTjeneste {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    InntektsmeldingDtoTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, MottatteDokumentRepository mottatteDokumentRepository, VirksomhetTjeneste virksomhetTjeneste,
                               ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                               BehandlingRepository behandlingRepository,
                               FagsakRepository fagsakRepository,
                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    InntektsmeldingDtoTjeneste() {
        //CDI
    }


    public List<FpSakInntektsmeldingDto> hentInntektsmeldingerForSak(Saksnummer saksnummer) {
        var sak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        var inntektArbeidYtelseGrunnlag = sak.flatMap(
                s -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(s.getId()))
            .flatMap(b -> inntektArbeidYtelseTjeneste.finnGrunnlag(b.getId()));

        var yrkesaktivitet = inntektArbeidYtelseGrunnlag
            .flatMap(InntektArbeidYtelseGrunnlag::getRegisterVersjon)
            .map(InntektArbeidYtelseAggregat::getAktørArbeid).orElse(List.of())
            .stream().filter(i -> i.getAktørId().equals(sak.get().getAktørId())).findFirst()
            .map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(List.of());

        var inntektsmeldinger = inntektArbeidYtelseGrunnlag
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(List.of())
            .stream().map(Inntektsmelding::getJournalpostId).toList();

        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(saksnummer)
            .stream()
            .map(i -> map(i, inntektsmeldinger.contains(i.getJournalpostId()), yrkesaktivitet))
            .collect(Collectors.toList());
    }

    private FpSakInntektsmeldingDto map(Inntektsmelding inntektsmelding, boolean erAktiv, Collection<Yrkesaktivitet> yrkesaktivitet) {
        var mottattTidspunkt = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId())
            .stream()
            .map(MottattDokument::getMottattTidspunkt)
            .min(LocalDateTime::compareTo)
            .orElseThrow();

        var naturalytelser = konverterAktivePerioderTilBortfaltePerioder(inntektsmelding.getNaturalYtelser());
        var refusjonsperioder = lagRefusjonsperioder(inntektsmelding);

        var arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(inntektsmelding.getArbeidsgiver());

        var stillingsprosent = yrkesaktivitet.stream()
            .filter(i->i.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()))
            .findFirst()
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler).orElse(List.of())
            .stream().filter(a -> a.getProsentsats() != null)
            .max(Comparator.comparing(a-> a.getPeriode().getFomDato())).map(AktivitetsAvtale::getProsentsats).map(Stillingsprosent::getVerdi)
            .orElse(null);

        return new FpSakInntektsmeldingDto(
            erAktiv,
            stillingsprosent,
            inntektsmelding.getInntektBeløp().getVerdi(),
            inntektsmelding.getRefusjonBeløpPerMnd() == null ? null : inntektsmelding.getRefusjonBeløpPerMnd().getVerdi(),
            arbeidsgiverOpplysninger.getNavn(),
            inntektsmelding.getArbeidsgiver().getIdentifikator(),
            inntektsmelding.getJournalpostId().getVerdi(),
            mottattTidspunkt,
            inntektsmelding.getStartDatoPermisjon().orElse(null),
            naturalytelser,
            refusjonsperioder
        );
    }

    public static List<FpSakInntektsmeldingDto.Refusjon> lagRefusjonsperioder(Inntektsmelding inntektsmelding) {
        var refusjon = inntektsmelding.getEndringerRefusjon().stream().map(r -> new FpSakInntektsmeldingDto.Refusjon(r.getRefusjonsbeløp().getVerdi(), r.getFom())).toList();
        var mutableRefusjon = new ArrayList<>(refusjon);

        // Representer opphøring av refusjon som en periode med 0 som refusjon
        if (inntektsmelding.getRefusjonOpphører() != null && !TIDENES_ENDE.equals(inntektsmelding.getRefusjonOpphører() )) {
            mutableRefusjon.add(new FpSakInntektsmeldingDto.Refusjon(new BigDecimal(0), inntektsmelding.getRefusjonOpphører().plusDays(1)));
        }

        mutableRefusjon.sort(Comparator.comparing(FpSakInntektsmeldingDto.Refusjon::fomDato));

        return mutableRefusjon;
    }

    public static List<FpSakInntektsmeldingDto.Naturalytelse> konverterAktivePerioderTilBortfaltePerioder(List<NaturalYtelse> aktiveNaturalytelser) {
        var gruppertPåType = aktiveNaturalytelser.stream()
            .collect(Collectors.groupingBy(NaturalYtelse::getType));

        List<FpSakInntektsmeldingDto.Naturalytelse> bortfalteNaturalytelser = new ArrayList<>();

        gruppertPåType.forEach((key, value) -> {
            var sortert = value.stream()
                .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
                .toList();

            for (int i = 0; i < sortert.size(); i++) {
                var current = sortert.get(i);
                var next = (i + 1 < sortert.size()) ? sortert.get(i + 1) : null;

                LocalDate nyFom = current.getPeriode().getTomDato();
                LocalDate nyTom = (next != null) ? next.getPeriode().getFomDato() : null;

                if (nyFom.equals(TIDENES_ENDE)) {
                    continue;
                }

                var newYtelse = new FpSakInntektsmeldingDto.Naturalytelse(
                        nyFom.plusDays(1),
                        (nyTom != null) ? nyTom.minusDays(1) : TIDENES_ENDE,
                    current.getBeloepPerMnd().getVerdi(),
                    mapTilFpSakIMDTONaturalytelseType(current.getType())
                );

                bortfalteNaturalytelser.add(newYtelse);
            }
        });

        return bortfalteNaturalytelser;
    }

    private static FpSakInntektsmeldingDto.NaturalytelseType mapTilFpSakIMDTONaturalytelseType(NaturalYtelseType naturalytelseType) {
        return switch (naturalytelseType) {
            case ELEKTRISK_KOMMUNIKASJON -> FpSakInntektsmeldingDto.NaturalytelseType.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> FpSakInntektsmeldingDto.NaturalytelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> FpSakInntektsmeldingDto.NaturalytelseType.LOSJI;
            case KOST_DØGN -> FpSakInntektsmeldingDto.NaturalytelseType.KOST_DØGN;
            case BESØKSREISER_HJEMMET_ANNET -> FpSakInntektsmeldingDto.NaturalytelseType.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> FpSakInntektsmeldingDto.NaturalytelseType.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> FpSakInntektsmeldingDto.NaturalytelseType.RENTEFORDEL_LÅN;
            case BIL -> FpSakInntektsmeldingDto.NaturalytelseType.BIL;
            case KOST_DAGER -> FpSakInntektsmeldingDto.NaturalytelseType.KOST_DAGER;
            case BOLIG -> FpSakInntektsmeldingDto.NaturalytelseType.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> FpSakInntektsmeldingDto.NaturalytelseType.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> FpSakInntektsmeldingDto.NaturalytelseType.FRI_TRANSPORT;
            case OPSJONER -> FpSakInntektsmeldingDto.NaturalytelseType.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> FpSakInntektsmeldingDto.NaturalytelseType.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> FpSakInntektsmeldingDto.NaturalytelseType.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> FpSakInntektsmeldingDto.NaturalytelseType.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> FpSakInntektsmeldingDto.NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> FpSakInntektsmeldingDto.NaturalytelseType.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> FpSakInntektsmeldingDto.NaturalytelseType.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
            case UDEFINERT -> throw new IllegalStateException("Kunne ikke mappe NaturalytelseType til FpSakInntektsmeldingDto.NaturalytelseType: " + naturalytelseType);
        };
    }
}
