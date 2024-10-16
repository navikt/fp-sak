package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingDtoTjeneste;
import no.nav.vedtak.konfig.Tid;

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


    public List<InntektsmeldingDto> hentInntektsmeldingerForSak(Saksnummer saksnummer) {
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

    private InntektsmeldingDto map(Inntektsmelding inntektsmelding, boolean erAktiv, Collection<Yrkesaktivitet> yrkesaktivitet) {
        var mottattTidspunkt = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId())
            .stream()
            .map(MottattDokument::getMottattTidspunkt)
            .min(LocalDateTime::compareTo)
            .orElseThrow();

        var motatteDokument = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId()).stream().findFirst();
        var kontaktinfo = motatteDokument.flatMap(ArbeidOgInntektsmeldingDtoTjeneste::trekkUtKontaktInfo);

        var naturalytelser = konverterAktivePerioderTilBortfaltePerioder(inntektsmelding.getNaturalYtelser());
        var refusjon = inntektsmelding.getEndringerRefusjon().stream().map(r -> new InntektsmeldingDto.Refusjon(r.getRefusjonsbeløp().getVerdi(), r.getFom())).toList();

        // Representer opphøring av refusjon som en periode med 0 som refusjon
        if (inntektsmelding.getRefusjonOpphører() != null && !Tid.TIDENES_ENDE.equals(inntektsmelding.getRefusjonOpphører() )) {
            refusjon = new ArrayList<>(refusjon);
            refusjon.add(new InntektsmeldingDto.Refusjon(new BigDecimal(0), inntektsmelding.getRefusjonOpphører().plusDays(1)));
        }

        var arbeidsgiverNavn = arbeidsgiverTjeneste.hent(inntektsmelding.getArbeidsgiver());

        var stillingsprosent = yrkesaktivitet.stream()
            .filter(i->i.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()))
            .findFirst()
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler).orElse(List.of())
            .stream().filter(a -> a.getProsentsats() != null)
            .max(Comparator.comparing(a-> a.getPeriode().getFomDato())).map(AktivitetsAvtale::getProsentsats).map(Stillingsprosent::getVerdi)
            .orElse(null);

        return new InntektsmeldingDto(
            erAktiv,
            stillingsprosent,
            inntektsmelding.getInntektBeløp().getVerdi(),
            inntektsmelding.getRefusjonBeløpPerMnd() == null ? null : inntektsmelding.getRefusjonBeløpPerMnd().getVerdi(),
            arbeidsgiverNavn.getNavn(),
            kontaktinfo.map(KontaktinformasjonIM::kontaktPerson).orElse(null),
            kontaktinfo.map(KontaktinformasjonIM::kontaktTelefonNummer).orElse(null),
            inntektsmelding.getJournalpostId(),
            mottattTidspunkt,
            inntektsmelding.getStartDatoPermisjon().orElse(null),
            naturalytelser,
            refusjon
        );
    }

    public static List<InntektsmeldingDto.NaturalYtelse> konverterAktivePerioderTilBortfaltePerioder(List<NaturalYtelse> aktiveNaturalytelser) {
        var gruppertPåType = aktiveNaturalytelser.stream()
            .collect(Collectors.groupingBy(NaturalYtelse::getType));

        List<InntektsmeldingDto.NaturalYtelse> bortfalteNaturalytelser = new ArrayList<>();

        gruppertPåType.forEach((key, value) -> {
            var sortert = value.stream()
                .sorted(Comparator.comparing(a -> a.getPeriode().getFomDato()))
                .collect(Collectors.toList());
            Collections.reverse(sortert);

            for (int i = 0; i < sortert.size(); i++) {
                var current = sortert.get(i);
                var next = (i + 1 < sortert.size()) ? sortert.get(i + 1) : null;

                LocalDate nyFom = current.getPeriode().getTomDato();
                LocalDate nyTom = (next != null) ? next.getPeriode().getFomDato() : null;

                if (nyFom.equals(TIDENES_ENDE)) {
                    continue;
                }

                var newYtelse = new InntektsmeldingDto.NaturalYtelse(
                        nyFom.plusDays(1),
                        (nyTom != null) ? nyTom.minusDays(1) : TIDENES_ENDE,
                    current.getBeloepPerMnd().getVerdi(),
                    current.getType()
                );

                bortfalteNaturalytelser.add(newYtelse);
            }
        });

        return bortfalteNaturalytelser;
    }
}
