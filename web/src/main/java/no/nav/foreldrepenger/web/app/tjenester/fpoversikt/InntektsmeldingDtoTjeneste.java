package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingDtoTjeneste;

@ApplicationScoped
class InntektsmeldingDtoTjeneste {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VirksomhetTjeneste virksomhetTjeneste;

    @Inject
    InntektsmeldingDtoTjeneste(InntektsmeldingTjeneste inntektsmeldingTjeneste, MottatteDokumentRepository mottatteDokumentRepository, VirksomhetTjeneste virksomhetTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    InntektsmeldingDtoTjeneste() {
        //CDI
    }


    public List<InntektsmeldingDto> hentInntektsmeldingerForSak(Saksnummer saksnummer) {
        return inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(saksnummer)
            .stream()
            .map(this::map)
            .collect(Collectors.toList());
    }

    private InntektsmeldingDto map(Inntektsmelding inntektsmelding) {
        var arbeidsgiver = new Arbeidsgiver(inntektsmelding.getArbeidsgiver().getIdentifikator());
        var mottattTidspunkt = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId())
            .stream()
            .map(MottattDokument::getMottattTidspunkt)
            .min(LocalDateTime::compareTo)
            .orElseThrow(); //TODO

        var motatteDokument = mottatteDokumentRepository.hentMottattDokument(inntektsmelding.getJournalpostId()).stream().findFirst();
        var kontaktinfo = motatteDokument.flatMap(ArbeidOgInntektsmeldingDtoTjeneste::trekkUtKontaktInfo);

        var naturalytelser = inntektsmelding.getNaturalYtelser().stream().map(n -> new InntektsmeldingDto.NaturalYtelse(n.getPeriode().getFomDato(), n.getPeriode().getTomDato(),n.getBeloepPerMnd().getVerdi(), n.getType())).toList();
        var refusjon = inntektsmelding.getEndringerRefusjon().stream().map(r -> new InntektsmeldingDto.Refusjon(r.getRefusjonsbeløp().getVerdi(), r.getFom())).toList();

        // TODO: whatdo hvis feiler?
        var virksomhet = virksomhetTjeneste.hentOrganisasjon(inntektsmelding.getArbeidsgiver().getOrgnr());

        return new InntektsmeldingDto(
            false,
            inntektsmelding.getInntektBeløp().getVerdi(),
            inntektsmelding.getRefusjonBeløpPerMnd() == null ? null : inntektsmelding.getRefusjonBeløpPerMnd().getVerdi(),
            virksomhet.getNavn(),
            kontaktinfo.map(KontaktinformasjonIM::kontaktPerson).orElse(null),
            kontaktinfo.map(KontaktinformasjonIM::kontaktTelefonNummer).orElse(null),
            inntektsmelding.getJournalpostId(),
            inntektsmelding.getInnsendingstidspunkt(),
            mottattTidspunkt,
            inntektsmelding.getStartDatoPermisjon().orElse(null),
            naturalytelser,
            refusjon
        );
    }
}
