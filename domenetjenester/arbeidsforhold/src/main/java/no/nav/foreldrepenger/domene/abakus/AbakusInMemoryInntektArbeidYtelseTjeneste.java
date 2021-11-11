package no.nav.foreldrepenger.domene.abakus;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;

import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYDiffsjekker;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * In-memory - legger kun grunnlag i minne (lagrer ikke i noe lager). Brukes
 * under forflytting til Abakus til å erstatte tester som går mot
 * {@link no.nav.foreldrepenger.behandlingslager.abakus.inntektarbeidytelse.InntektArbeidYtelseRepository}.
 * NB: Skal kun brukes for tester.
 * <p>
 * Definer som alternative i beans.xml (i src/test/resources/META-INF) i modul
 * som skal bruke
 * <p>
 * <p>
 * Legg inn i fil <code>src/test/resources/META-INF</code> for å aktivere for
 * enhetstester:
 * <p>
 * <code>
 * &lt;alternatives&gt;<br>
 * &lt;class&gt;no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste&lt;/class&gt;<br>
 * &lt;/alternatives&gt;<br>
 * </code>
 */
@RequestScoped
@Alternative
public class AbakusInMemoryInntektArbeidYtelseTjeneste implements InntektArbeidYtelseTjeneste {

    private Map<Long, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private List<InntektArbeidYtelseGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public AbakusInMemoryInntektArbeidYtelseTjeneste() {
    }

    private static String getCallerMethod() {
        var frames = StackWalker.getInstance().walk(s -> s.limit(2).collect(Collectors.toList()));
        return frames.get(1).getMethodName();
    }

    @Override
    public Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId);
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId).orElseThrow();
    }

    @Override
    public InntektArbeidYtelseGrunnlagDto hentGrunnlagKontrakt(Long behandlingId) {
        // Brukes kun av swagger, så greit å returnere null her
        return null;
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlagPåId(Long behandlingId, UUID inntektArbeidYtelseGrunnlagId) {
        return grunnlag.stream().filter(g -> Objects.equals(g.getEksternReferanse(), inntektArbeidYtelseGrunnlagId))
                .findFirst().orElseThrow();
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId) {
        var origAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(fraBehandlingId);
        origAggregat.ifPresent(orig -> {
            var entitet = new InntektArbeidYtelseGrunnlag(orig);
            lagreOgFlush(tilBehandlingId, entitet);
        });
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long fraBehandlingId, Long tilBehandlingId) {
        var origAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(fraBehandlingId);
        origAggregat.ifPresent(orig -> {
            var entitet = new InntektArbeidYtelseGrunnlag(orig);
            lagreOgFlush(tilBehandlingId, entitet);
        });
    }

    @Override
    public List<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer) {
        return new ArrayList<>(hentInntektsmeldinger(saksnummer).getAlleInntektsmeldinger());
    }

    @Override
    public List<RefusjonskravDato> hentRefusjonskravDatoerForSak(Saksnummer saksnummer) {
        return hentInntektsmeldinger(saksnummer).getAlleInntektsmeldinger().stream()
                .filter(im -> !im.getRefusjonBeløpPerMnd().erNullEllerNulltall() || !im.getEndringerRefusjon().isEmpty())
                .collect(Collectors.groupingBy(Inntektsmelding::getArbeidsgiver)).entrySet().stream()
                .map(entry -> {
                    var førsteInnsendingAvRefusjon = entry.getValue().stream().map(Inntektsmelding::getInnsendingstidspunkt)
                            .min(Comparator.naturalOrder()).map(LocalDateTime::toLocalDate).orElse(TIDENES_ENDE);
                    var førsteDatoForRefusjon = entry.getValue().stream()
                            .map(im -> {
                                if (!im.getRefusjonBeløpPerMnd().erNullEllerNulltall()) {
                                    return im.getStartDatoPermisjon().orElse(TIDENES_ENDE);
                                }
                                return im.getEndringerRefusjon().stream()
                                        .filter(er -> !er.getRefusjonsbeløp().erNullEllerNulltall())
                                        .min(Comparator.comparing(Refusjon::getFom))
                                        .map(Refusjon::getFom).orElse(TIDENES_ENDE);
                            }).min(Comparator.naturalOrder()).orElse(TIDENES_ENDE);
                    return new RefusjonskravDato(entry.getKey(), førsteDatoForRefusjon, førsteInnsendingAvRefusjon,
                            entry.getValue().stream().anyMatch(im -> !im.getRefusjonBeløpPerMnd().erNullEllerNulltall()));
                }).collect(Collectors.toList());
    }

    @Override
    public void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        var grunnlagBuilder = getGrunnlagBuilder(behandlingId, builder);

        final var informasjon = grunnlagBuilder.getInformasjon();

        // lagre reserverte interne referanser opprettet tidligere
        builder.getNyeInternArbeidsforholdReferanser()
                .forEach(aref -> informasjon.opprettNyReferanse(aref.getArbeidsgiver(), aref.getInternReferanse(), aref.getEksternReferanse()));

        lagreOgFlush(behandlingId, grunnlagBuilder.build());
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return opprettBuilderFor(VersjonType.REGISTER, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(UUID behandlingUuid, UUID angittReferanse,
            LocalDateTime angittOpprettetTidspunkt) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingUuid);
        return opprettBuilderFor(VersjonType.REGISTER, angittReferanse, angittOpprettetTidspunkt, iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(UUID behandlingUuid, UUID angittReferanse,
            LocalDateTime angittOpprettetTidspunkt) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingUuid);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, angittReferanse, angittOpprettetTidspunkt, iayGrunnlag);
    }

    private static Optional<InntektArbeidYtelseGrunnlag> hentInntektArbeidYtelseGrunnlagForBehandling(UUID behandlingUUid) {
        var iayGrunnlag = getAktivtInntektArbeidGrunnlag(behandlingUUid);
        return iayGrunnlag.isPresent() ? Optional.of(iayGrunnlag.get()) : Optional.empty();
    }

    private InntektArbeidYtelseAggregatBuilder opprettBuilderFor(VersjonType versjonType, UUID angittReferanse, LocalDateTime opprettetTidspunkt,
            Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        var grunnlagBuilder = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag);
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder");
        var aggregat = Optional.ofNullable(grunnlagBuilder.getKladd()); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(aggregat, "aggregat"); // NOSONAR $NON-NLS-1$
        if (aggregat.isPresent()) {
            final var aggregat1 = aggregat.get();
            return InntektArbeidYtelseAggregatBuilder.builderFor(hentRiktigVersjon(versjonType, aggregat1), angittReferanse, opprettetTidspunkt,
                    versjonType);
        }
        throw new IllegalArgumentException("aggregat kan ikke være null: " + angittReferanse);
    }

    private Optional<InntektArbeidYtelseAggregat> hentRiktigVersjon(VersjonType versjonType, InntektArbeidYtelseGrunnlag aggregat) {
        if (versjonType == VersjonType.REGISTER) {
            return aggregat.getRegisterVersjon();
        }
        if (versjonType == VersjonType.SAKSBEHANDLET) {
            return aggregat.getSaksbehandletVersjon();
        }
        throw new IllegalStateException("Kunne ikke finne riktig versjon av InntektArbeidYtelseGrunnlag");
    }

    @Override
    public void fjernSaksbehandletVersjon(Long behandlingId) {
        var entitetOpt = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        if (entitetOpt.isPresent()) {
            var entitet = entitetOpt.get();
            var builder = new InMemoryInntektArbeidYtelseGrunnlagBuilder(entitet);
            builder.fjernSaksbehandlet();
            lagreOgFlush(behandlingId, entitet);
        }
    }

    @Override
    public void lagreArbeidsforhold(Long behandlingId, AktørId søkerAktørId, ArbeidsforholdInformasjonBuilder informasjon) {
        Objects.requireNonNull(informasjon, "informasjon"); // NOSONAR
        var builder = opprettGrunnlagBuilderFor(behandlingId);

        builder.ryddOppErstattedeArbeidsforhold(søkerAktørId, informasjon.getReverserteErstattArbeidsforhold());
        builder.ryddOppErstattedeArbeidsforhold(søkerAktørId, informasjon.getErstattArbeidsforhold());
        builder.medInformasjon(informasjon.build());

        lagreOgFlush(behandlingId, builder.build());
    }

    @Override
    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return;
        }
        var inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);

        var iayGrunnlag = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
        iayGrunnlag.medOppgittOpptjening(oppgittOpptjening);

        lagreOgFlush(behandlingId, iayGrunnlag.build());
    }

    @Override
    public SakInntektsmeldinger hentInntektsmeldinger(Saksnummer saksnummer) {
        var alleGrunnlag = grunnlag;
        var resultat = new SakInntektsmeldinger(saksnummer);
        for (var iayg : alleGrunnlag) {
            var ims = iayg.getInntektsmeldinger();
            if (ims.isPresent()) {
                for (var behId : alleBehandlingMedGrunnlag(iayg.getEksternReferanse())) {
                    for (var im : ims.get().getAlleInntektsmeldinger()) {
                        resultat.leggTil(behId, iayg.getEksternReferanse(), iayg.getOpprettetTidspunkt(), im);
                    }
                }
            }
        }
        return resultat;
    }

    @Override
    public void lagreOverstyrtArbeidsforhold(Long behandlingId, AktørId søkerAktørId, ArbeidsforholdInformasjonBuilder informasjon) {
        Objects.requireNonNull(informasjon, "informasjon"); // NOSONAR
        var builder = opprettGrunnlagBuilderFor(behandlingId);

        builder.ryddOppErstattedeArbeidsforhold(søkerAktørId, informasjon.getReverserteErstattArbeidsforhold());
        builder.ryddOppErstattedeArbeidsforhold(søkerAktørId, informasjon.getErstattArbeidsforhold());
        builder.medInformasjon(informasjon.build());

        lagreOgFlush(behandlingId, builder.build());
    }

    @Override
    public List<Inntektsmelding> finnInntektsmeldingDiff(BehandlingReferanse referanse) {
        return Collections.emptyList();
    }

    @Override
    public void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, Collection<InntektsmeldingBuilder> builders) {
        Objects.requireNonNull(builders, "builders"); // NOSONAR
        var builder = opprettGrunnlagBuilderFor(behandlingId);
        final var inntektsmeldinger = builder.getInntektsmeldinger();

        for (var inntektsmeldingBuilder : builders) {
            final var informasjon = builder.getInformasjon();
            konverterEksternArbeidsforholdRefTilInterne(inntektsmeldingBuilder, informasjon);

            var inntektsmelding = inntektsmeldingBuilder.build();
            final var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(informasjon);

            // Kommet inn inntektsmelding på arbeidsforhold som vi har gått videre med uten
            // inntektsmelding?
            if (kommetInntektsmeldingPåArbeidsforholdHvorViTidligereBehandletUtenInntektsmelding(inntektsmelding, informasjon)) {
                informasjonBuilder.fjernOverstyringVedrørende(inntektsmeldingBuilder.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef());
            }
            // Gjelder tilfeller der det først har kommet inn inntektsmelding uten id, også
            // kommer det inn en inntektsmelding med spesifik id
            // nullstiller da valg gjort i 5080 slik at saksbehandler må ta stilling til
            // aksjonspunktet på nytt.
            var arbeidsgiverSomMåTilbakestilles = utledeArbeidsgiverSomMåTilbakestilles(inntektsmelding, informasjon);
            arbeidsgiverSomMåTilbakestilles.ifPresent(informasjonBuilder::fjernOverstyringerSomGjelder);

            builder.medInformasjon(informasjonBuilder.build());
            inntektsmeldinger.leggTil(inntektsmelding);
        }
        builder.setInntektsmeldinger(inntektsmeldinger);
        lagreOgFlush(behandlingId, builder.build());
    }

    private static Optional<Arbeidsgiver> utledeArbeidsgiverSomMåTilbakestilles(Inntektsmelding inntektsmelding,
            ArbeidsforholdInformasjon informasjon) {
        if (inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()) {
            return informasjon.getOverstyringer().stream()
                    .filter(o -> o.getArbeidsgiver().equals(inntektsmelding.getArbeidsgiver()) &&
                            !o.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
                    .map(ArbeidsforholdOverstyring::getArbeidsgiver)
                    .findFirst();
        }
        return Optional.empty();
    }

    private static void konverterEksternArbeidsforholdRefTilInterne(InntektsmeldingBuilder inntektsmeldingBuilder,
            final ArbeidsforholdInformasjon informasjon) {
        if (inntektsmeldingBuilder.getEksternArbeidsforholdRef().isPresent()) {
            var ekstern = inntektsmeldingBuilder.getEksternArbeidsforholdRef().get();
            var intern = inntektsmeldingBuilder.getInternArbeidsforholdRef();
            if (ekstern.gjelderForSpesifiktArbeidsforhold()) {
                if (!intern.get().gjelderForSpesifiktArbeidsforhold()) {
                    // lag ny intern id siden vi i
                    var internId = informasjon.finnEllerOpprett(inntektsmeldingBuilder.getArbeidsgiver(), ekstern);
                    inntektsmeldingBuilder.medArbeidsforholdId(internId);
                } else {
                    // registrer ekstern <-> intern mapping for allerede opprettet intern id
                    informasjon.opprettNyReferanse(inntektsmeldingBuilder.getArbeidsgiver(), intern.get(), ekstern);
                }
            } else {
                // sikre at også intern referanse for builder er generell
                intern.ifPresent(v -> {
                    if (v.getReferanse() != null) {
                        throw new IllegalStateException("Har ekstern referanse som gjelder alle arbeidsforhold, men intern er spesifikk: " + v);
                    }
                });
            }
        } // else do nothing
    }

    private boolean kommetInntektsmeldingPåArbeidsforholdHvorViTidligereBehandletUtenInntektsmelding(Inntektsmelding inntektsmelding,
            ArbeidsforholdInformasjon informasjon) {
        return informasjon.getOverstyringer()
                .stream()
                .anyMatch(ov -> (ov.kreverIkkeInntektsmelding() || ov.getHandling().equals(ArbeidsforholdHandlingType.IKKE_BRUK))
                        && ov.getArbeidsgiver().equals(inntektsmelding.getArbeidsgiver())
                        && ov.getArbeidsforholdRef().gjelderFor(inntektsmelding.getArbeidsforholdRef()));
    }

    private Set<Long> alleBehandlingMedGrunnlag(UUID grunnlagId) {
        return indeksBehandlingTilGrunnlag.entrySet().stream()
                .filter(e -> e.getValue().contains(grunnlagId))
                .map(e -> e.getKey())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUUid) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED (mangler kobling til behandlingUUid): #" + getCallerMethod());
    }

    private static Optional<InntektArbeidYtelseGrunnlag> getAktivtInntektArbeidGrunnlag(@SuppressWarnings("unused") UUID behandlingId) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED (mangler kobling til behandlingUUid): #" + getCallerMethod());
    }

    private Optional<InntektArbeidYtelseGrunnlag> getAktivtInntektArbeidGrunnlag(Long behandlingId) {
        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());
        if (behGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        return behGrunnlag.stream().map(grId -> hentGrunnlagPåId(behandlingId, grId))
                .filter(gr -> gr.isAktiv())
                .findFirst();
    }

    private InMemoryInntektArbeidYtelseGrunnlagBuilder getGrunnlagBuilder(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        Objects.requireNonNull(builder, "inntektArbeidYtelserBuilder"); // NOSONAR
        var opptjeningAggregatBuilder = opprettGrunnlagBuilderFor(behandlingId);
        opptjeningAggregatBuilder.medData(builder);
        return opptjeningAggregatBuilder;
    }

    private Optional<InntektArbeidYtelseGrunnlag> hentInntektArbeidYtelseGrunnlagForBehandling(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId);
    }

    private void lagreGrunnlag(InntektArbeidYtelseGrunnlag nyttGrunnlag, Long behandlingId) {
        var entitet = nyttGrunnlag;

        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());

        setField(entitet, "behandlingId", behandlingId);

        behGrunnlag.push(entitet.getEksternReferanse());
        grunnlag.add(entitet);
    }

    private void lagreOgFlush(Long behandlingId, InntektArbeidYtelseGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        if (nyttGrunnlag == null) {
            return;
        }
        var tidligereAggregat = getAktivtInntektArbeidGrunnlag(behandlingId);
        if (tidligereAggregat.isPresent()) {
            var entitet = tidligereAggregat.get();
            if (new IAYDiffsjekker(false).getDiffEntity().diff(entitet, nyttGrunnlag).isEmpty()) {
                return;
            }
            setField(entitet, "aktiv", false);
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        } else {
            lagreGrunnlag(nyttGrunnlag, behandlingId);
        }
    }

    private InMemoryInntektArbeidYtelseGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        var inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
    }

    private static void setField(Object entitet, String field, Object val) {
        try {
            var fld = entitet.getClass().getDeclaredField(field);
            fld.setAccessible(true);
            fld.set(entitet, val);
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new UnsupportedOperationException("Kan ikke sette felt: " + field + " på entitet: " + entitet.getClass() + " til " + val, e);
        }
    }

    /**
     * lagd for å eksponere builder metoder lokalt til denne tjeneste
     * implementasjonen.
     */
    private static class InMemoryInntektArbeidYtelseGrunnlagBuilder extends InntektArbeidYtelseGrunnlagBuilder {
        private InMemoryInntektArbeidYtelseGrunnlagBuilder(InntektArbeidYtelseGrunnlag kladd) {
            super(kladd);
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder nytt() {
            return ny(UUID.randomUUID(), LocalDateTime.now());
        }

        /**
         * Opprett ny versjon av grunnlag med angitt assignet grunnlagReferanse og
         * opprettetTidspunkt.
         */
        public static InMemoryInntektArbeidYtelseGrunnlagBuilder ny(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
            return new InMemoryInntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(grunnlagReferanse, opprettetTidspunkt));
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder oppdatere(InntektArbeidYtelseGrunnlag kladd) {
            return new InMemoryInntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(kladd));
        }

        public static InMemoryInntektArbeidYtelseGrunnlagBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> kladd) {
            return kladd.map(InMemoryInntektArbeidYtelseGrunnlagBuilder::oppdatere).orElseGet(InMemoryInntektArbeidYtelseGrunnlagBuilder::nytt);
        }

        @Override
        public void fjernSaksbehandlet() {
            super.fjernSaksbehandlet();
        }

        @Override
        public void ryddOppErstattedeArbeidsforhold(AktørId søker,
                List<ArbeidsforholdInformasjonBuilder.ArbeidsgiverForholdRefs> erstattArbeidsforhold) {
            super.ryddOppErstattedeArbeidsforhold(søker, erstattArbeidsforhold);
        }

        @Override
        public InntektArbeidYtelseGrunnlag getKladd() {
            return super.getKladd();
        }

        @Override
        public InntektsmeldingAggregat getInntektsmeldinger() {
            return super.getInntektsmeldinger();
        }

    }
}
