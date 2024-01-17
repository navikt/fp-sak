package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBElement;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.InntektsmeldingFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.NaturalytelseKodeliste;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakInnsendingKodeliste;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakUtsettelseKodeliste;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._201809.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20180924.EndringIRefusjonsListe;

@NamespaceRef(InntektsmeldingConstants.NAMESPACE)
@ApplicationScoped
public class InntektsmeldingOversetter implements MottattDokumentOversetter<InntektsmeldingWrapper> {

    private static final LocalDate TIDENES_BEGYNNELSE = LocalDate.of(1, Month.JANUARY, 1);
    private static final Map<ÅrsakInnsendingKodeliste, InntektsmeldingInnsendingsårsak> INNSENDINGSÅRSAK_MAP;

    static {
        INNSENDINGSÅRSAK_MAP = new EnumMap<>(ÅrsakInnsendingKodeliste.class);
        INNSENDINGSÅRSAK_MAP.put(ÅrsakInnsendingKodeliste.ENDRING, InntektsmeldingInnsendingsårsak.ENDRING);
        INNSENDINGSÅRSAK_MAP.put(ÅrsakInnsendingKodeliste.NY, InntektsmeldingInnsendingsårsak.NY);
    }

    private VirksomhetTjeneste virksomhetTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    InntektsmeldingOversetter() {
        // for CDI proxy
    }

    @Inject
    public InntektsmeldingOversetter(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     VirksomhetTjeneste virksomhetTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    @Override
    public void trekkUtDataOgPersister(InntektsmeldingWrapper wrapper,
                                       MottattDokument mottattDokument,
                                       Behandling behandling,
                                       Optional<LocalDate> gjelderFra) {
        var aarsakTilInnsending = wrapper.getSkjema().getSkjemainnhold().getAarsakTilInnsending();
        var innsendingsårsak = aarsakTilInnsending.isEmpty() ? InntektsmeldingInnsendingsårsak.UDEFINERT : INNSENDINGSÅRSAK_MAP
            .get(ÅrsakInnsendingKodeliste.fromValue(aarsakTilInnsending));

        var builder = InntektsmeldingBuilder.builder();

        mapInnsendingstidspunkt(wrapper, mottattDokument, builder);

        builder.medMottattDato(mottattDokument.getMottattDato());
        builder.medKildesystem(wrapper.getAvsendersystem());
        builder.medKanalreferanse(mottattDokument.getKanalreferanse());
        builder.medJournalpostId(mottattDokument.getJournalpostId());

        mapArbeidsgiver(wrapper, builder);

        builder.medNærRelasjon(wrapper.getErNærRelasjon());
        builder.medInntektsmeldingaarsak(innsendingsårsak);

        mapArbeidsforholdOgBeløp(wrapper, builder);
        mapNaturalYtelser(wrapper, builder);
        mapGradering(wrapper, builder);
        mapFerie(wrapper, builder);
        mapUtsettelse(wrapper, builder);
        mapRefusjon(wrapper, builder);

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(),
            builder);
    }

    private void mapArbeidsforholdOgBeløp(InntektsmeldingWrapper wrapper,
                                          InntektsmeldingBuilder builder) {
        var arbeidsforhold = wrapper.getArbeidsforhold();
        if (arbeidsforhold.isPresent()) {
            var arbeidsforholdet = arbeidsforhold.get();
            var arbeidsforholdId = arbeidsforholdet.getArbeidsforholdId();
            if (arbeidsforholdId != null) {
                var arbeidsforholdRef = EksternArbeidsforholdRef.ref(arbeidsforholdId.getValue());
                builder.medArbeidsforholdId(arbeidsforholdRef);
            }
            builder.medBeløp(arbeidsforholdet.getBeregnetInntekt().getValue().getBeloep().getValue())
                .medStartDatoPermisjon(wrapper.getStartDatoPermisjon().orElse(null));
        } else {
            throw InntektsmeldingFeil.manglendeInformasjon();
        }
    }

    private void mapArbeidsgiver(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        var orgNummer = wrapper.getArbeidsgiver().getVirksomhetsnummer();
        @SuppressWarnings("unused") var virksomhet = virksomhetTjeneste.hentOrganisasjon(orgNummer);
        builder.medArbeidsgiver(Arbeidsgiver.virksomhet(orgNummer));
    }

    private void mapInnsendingstidspunkt(InntektsmeldingWrapper wrapper,
                                         MottattDokument mottattDokument,
                                         InntektsmeldingBuilder builder) {
        var innsendingstidspunkt = wrapper.getInnsendingstidspunkt();
        if (innsendingstidspunkt.isPresent()) { // LPS
            builder.medInnsendingstidspunkt(innsendingstidspunkt.get());
        } else if (mottattDokument.getMottattTidspunkt() != null) { // Altinn
            builder.medInnsendingstidspunkt(mottattDokument.getMottattTidspunkt());
        } else {
            builder.medInnsendingstidspunkt(LocalDateTime.now());
        }
    }

    private void mapRefusjon(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        var optionalRefusjon = wrapper.getRefusjon();
        if (optionalRefusjon.isPresent()) {
            var refusjon = optionalRefusjon.get();
            if (refusjon.getRefusjonsopphoersdato() != null) {
                builder.medRefusjon(refusjon.getRefusjonsbeloepPrMnd().getValue(),
                    refusjon.getRefusjonsopphoersdato().getValue());
            } else if (refusjon.getRefusjonsbeloepPrMnd() != null) {
                builder.medRefusjon(refusjon.getRefusjonsbeloepPrMnd().getValue());
            }

            // Map endring i refusjon
            Optional.ofNullable(refusjon.getEndringIRefusjonListe())
                .map(JAXBElement::getValue)
                .map(EndringIRefusjonsListe::getEndringIRefusjon)
                .orElse(Collections.emptyList())
                .forEach(eir -> builder.leggTil(
                    new Refusjon(eir.getRefusjonsbeloepPrMnd().getValue(), eir.getEndringsdato().getValue())));

        }
    }

    private void mapUtsettelse(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        for (var detaljer : wrapper.getUtsettelser()) {
            var årsakUtsettelse = ÅrsakUtsettelseKodeliste.fromValue(detaljer.getAarsakTilUtsettelse().getValue());
            var årsak = UtsettelseÅrsak.fraKode(årsakUtsettelse.name());
            builder.leggTil(UtsettelsePeriode.utsettelse(detaljer.getPeriode().getValue().getFom().getValue(),
                detaljer.getPeriode().getValue().getTom().getValue(), årsak));
        }
    }

    private void mapFerie(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        for (var periode : wrapper.getAvtaltFerie()) {
            builder.leggTil(UtsettelsePeriode.ferie(periode.getFom().getValue(), periode.getTom().getValue()));
        }
    }

    private void mapNaturalYtelser(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        // Ved gjenopptakelse gjelder samme beløp
        Map<NaturalYtelseType, BigDecimal> beløp = new EnumMap<>(NaturalYtelseType.class);
        for (var detaljer : wrapper.getOpphørelseAvNaturalytelse()) {
            var naturalytelse = NaturalytelseKodeliste.fromValue(detaljer.getNaturalytelseType().getValue());
            var ytelseType = NaturalYtelseType.finnForKodeverkEiersKode(naturalytelse.value());
            beløp.put(ytelseType, detaljer.getBeloepPrMnd().getValue());
            var bortfallFom = detaljer.getFom().getValue();
            var naturalytelseTom = bortfallFom.minusDays(1);
            builder.leggTil(new NaturalYtelse(TIDENES_BEGYNNELSE, naturalytelseTom, beløp.get(ytelseType), ytelseType));
        }

        for (var detaljer : wrapper.getGjenopptakelserAvNaturalytelse()) {
            var naturalytelse = NaturalytelseKodeliste.fromValue(detaljer.getNaturalytelseType().getValue());
            var ytelseType = NaturalYtelseType.finnForKodeverkEiersKode(naturalytelse.value());
            builder.leggTil(
                new NaturalYtelse(detaljer.getFom().getValue(), Tid.TIDENES_ENDE, beløp.get(ytelseType), ytelseType));
        }
    }

    private void mapGradering(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        for (var detaljer : wrapper.getGradering()) {
            builder.leggTil(new Gradering(detaljer.getPeriode().getValue().getFom().getValue(),
                detaljer.getPeriode().getValue().getTom().getValue(),
                new BigDecimal(detaljer.getArbeidstidprosent().getValue())));
        }
    }
}
