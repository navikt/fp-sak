package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v2;

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

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.LukkForespørselForMottattImEvent;
import no.nav.foreldrepenger.domene.iay.modell.Gradering;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.UtsettelsePeriode;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.InntektsmeldingFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.NaturalytelseKodeliste;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakInnsendingKodeliste;
import no.nav.inntektsmelding.xml.kodeliste._2018xxyy.ÅrsakUtsettelseKodeliste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;
import no.seres.xsd.nav.inntektsmelding_m._201812.InntektsmeldingConstants;
import no.seres.xsd.nav.inntektsmelding_m._20181211.EndringIRefusjonsListe;

@NamespaceRef(InntektsmeldingConstants.NAMESPACE)
@ApplicationScoped
public class InntektsmeldingOversetter implements MottattDokumentOversetter<InntektsmeldingWrapper> {

    private static final LocalDate TIDENES_BEGYNNELSE = LocalDate.of(1, Month.JANUARY, 1);
    private static final Map<ÅrsakInnsendingKodeliste, InntektsmeldingInnsendingsårsak> INNSENDINGSÅRSAK_MAP;
    public static final String NAV_NO = "NAV_NO";
    private static final String OVERSTYRING_FPSAK = "OVERSTYRING_FPSAK";

    static {
        INNSENDINGSÅRSAK_MAP = new EnumMap<>(ÅrsakInnsendingKodeliste.class);
        INNSENDINGSÅRSAK_MAP.put(ÅrsakInnsendingKodeliste.ENDRING, InntektsmeldingInnsendingsårsak.ENDRING);
        INNSENDINGSÅRSAK_MAP.put(ÅrsakInnsendingKodeliste.NY, InntektsmeldingInnsendingsårsak.NY);
    }

    private VirksomhetTjeneste virksomhetTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BehandlingEventPubliserer behandlingEventPubliserer;

    InntektsmeldingOversetter() {
        // for CDI proxy
    }

    @Inject
    public InntektsmeldingOversetter(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     VirksomhetTjeneste virksomhetTjeneste,
                                     PersoninfoAdapter personinfoAdapter,
                                     BehandlingEventPubliserer behandlingEventPubliserer) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingEventPubliserer = behandlingEventPubliserer;
    }

    @Override
    public void trekkUtDataOgPersister(InntektsmeldingWrapper wrapper,
                                       MottattDokument mottattDokument,
                                       Behandling behandling,
                                       Optional<LocalDate> gjelderFra) {
        var aarsakTilInnsending = wrapper.getSkjema().getSkjemainnhold().getAarsakTilInnsending();
        var innsendingsårsak = aarsakTilInnsending.isEmpty() ? InntektsmeldingInnsendingsårsak.UDEFINERT : INNSENDINGSÅRSAK_MAP
            .get(ÅrsakInnsendingKodeliste.fromValue(aarsakTilInnsending));

        var imBuilder = InntektsmeldingBuilder.builder();

        mapInnsendingstidspunkt(wrapper, mottattDokument, imBuilder);

        var avsendersystem = wrapper.getAvsendersystem();
        imBuilder.medMottattDato(mottattDokument.getMottattDato());
        imBuilder.medKildesystem(avsendersystem);
        imBuilder.medKanalreferanse(mottattDokument.getKanalreferanse());
        imBuilder.medJournalpostId(mottattDokument.getJournalpostId());

        var arbeidsgiver = mapArbeidsgiver(wrapper, imBuilder);

        imBuilder.medNærRelasjon(wrapper.getErNærRelasjon());
        imBuilder.medInntektsmeldingaarsak(innsendingsårsak);

        mapArbeidsforholdOgBeløp(wrapper, imBuilder);
        mapNaturalYtelser(wrapper, imBuilder);
        mapGradering(wrapper, imBuilder);
        mapFerie(wrapper, imBuilder);
        mapUtsettelse(wrapper, imBuilder);
        mapRefusjon(wrapper, imBuilder);

        //Vi trenger ikke å lukke forespørsler som kommer fra arbeidsgiverportalen - de er allerede lukket
        if ((arbeidsgiver != null && arbeidsgiver.getErVirksomhet() && (avsendersystem == null || imFraLPSEllerAltinn(avsendersystem)))) {
            behandlingEventPubliserer.publiserBehandlingEvent(
                new LukkForespørselForMottattImEvent(behandling, new OrgNummer(imBuilder.getArbeidsgiver().getOrgnr())));
        }
        inntektsmeldingTjeneste.lagreInntektsmelding(imBuilder, behandling);
    }

    private boolean imFraLPSEllerAltinn(String avsendersystem) {
        return !(NAV_NO.equals(avsendersystem) || OVERSTYRING_FPSAK.equals(avsendersystem));
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

    private Arbeidsgiver mapArbeidsgiver(InntektsmeldingWrapper wrapper, InntektsmeldingBuilder builder) {
        var arbeidsgiverWrapper = wrapper.getArbeidsgiver();
        var arbeidsgiverPrivat = wrapper.getArbeidsgiverPrivat();
        if (arbeidsgiverWrapper.isPresent()) {
            var orgNummer = arbeidsgiverWrapper.get().getVirksomhetsnummer();
            @SuppressWarnings("unused") var virksomhet = virksomhetTjeneste.hentOrganisasjon(orgNummer);
            var arbeidsgiver = Arbeidsgiver.virksomhet(orgNummer);
            builder.medArbeidsgiver(arbeidsgiver);
            return arbeidsgiver;
        } else if (arbeidsgiverPrivat.isPresent()) {
            var aktørIdArbeidsgiver = personinfoAdapter.hentAktørForFnr(
                new PersonIdent(arbeidsgiverPrivat.get().getArbeidsgiverFnr()))
                .orElseThrow(() -> new TekniskException("FP-159641",
                    "Fant ikke personident for arbeidsgiver som er privatperson i PDL"));
            builder.medArbeidsgiver(Arbeidsgiver.person(aktørIdArbeidsgiver));
            return null;
        } else {
            throw new TekniskException("FP-183452", "Fant ikke informasjon om arbeidsgiver på inntektsmelding");
        }
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

            //Map endring i refusjon
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
            // FIXME (weak reference)
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
