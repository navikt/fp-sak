package no.nav.foreldrepenger.domene.abakus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;

import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
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
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
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

    private final Map<Long, Deque<UUID>> indeksBehandlingTilGrunnlag = new LinkedHashMap<>();
    private final List<InntektArbeidYtelseGrunnlag> grunnlag = new ArrayList<>();

    /**
     * CDI ctor for proxies.
     */
    public AbakusInMemoryInntektArbeidYtelseTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    private static String getCallerMethod() {
        var frames = StackWalker.getInstance().walk(s -> s.limit(2).toList());
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
        kopierGrunnlagFraEksisterendeBehandling(fraBehandlingId, tilBehandlingId);
    }

    @Override
    public List<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer) {
        return new ArrayList<>(hentInntektsmeldinger(saksnummer).getAlleInntektsmeldinger());
    }

    @Override
    public void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        var grunnlagBuilder = getGrunnlagBuilder(behandlingId, builder);

        var informasjon = grunnlagBuilder.getInformasjon();

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
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId) {
        var iayGrunnlag = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    private InntektArbeidYtelseAggregatBuilder opprettBuilderFor(VersjonType versjonType, UUID angittReferanse, LocalDateTime opprettetTidspunkt,
            Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        var grunnlagBuilder = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag);
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder");
        var aggregat = Optional.ofNullable(grunnlagBuilder.getKladd());
        Objects.requireNonNull(aggregat, "aggregat");
        if (aggregat.isPresent()) {
            var aggregat1 = aggregat.get();
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

    private void lagreArbeidsforhold(Long behandlingId, ArbeidsforholdInformasjonBuilder informasjon) {
        Objects.requireNonNull(informasjon, "informasjon");
        var builder = opprettGrunnlagBuilderFor(behandlingId);

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
    public void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return;
        }
        var inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);

        var iayGrunnlag = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
        iayGrunnlag.medOverstyrtOppgittOpptjening(oppgittOpptjening);

        lagreOgFlush(behandlingId, iayGrunnlag.build());

    }

    @Override
    public void lagreOppgittOpptjeningNullstillOverstyring(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjening) {
        if (oppgittOpptjening == null) {
            return;
        }
        var inntektArbeidAggregat = hentInntektArbeidYtelseGrunnlagForBehandling(behandlingId);

        var iayGrunnlag = InMemoryInntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidAggregat);
        iayGrunnlag.medOppgittOpptjening(oppgittOpptjening).medOverstyrtOppgittOpptjening(null);

        lagreOgFlush(behandlingId, iayGrunnlag.build());

    }

    private SakInntektsmeldinger hentInntektsmeldinger(Saksnummer saksnummer) {
        var resultat = new SakInntektsmeldinger(saksnummer);
        for (var iayg : grunnlag) {
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
    public void lagreOverstyrtArbeidsforhold(Long behandlingId, ArbeidsforholdInformasjonBuilder informasjon) {
        lagreArbeidsforhold(behandlingId, informasjon);
    }

    @Override
    public void lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId, Collection<InntektsmeldingBuilder> builders) {
        Objects.requireNonNull(builders, "builders");
        var builder = opprettGrunnlagBuilderFor(behandlingId);
        var inntektsmeldinger = builder.getInntektsmeldinger();

        for (var inntektsmeldingBuilder : builders) {
            var informasjon = builder.getInformasjon();
            konverterEksternArbeidsforholdRefTilInterne(inntektsmeldingBuilder, informasjon);

            var inntektsmelding = inntektsmeldingBuilder.build();
            var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(informasjon);

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
        var eksternRef = inntektsmeldingBuilder.getEksternArbeidsforholdRef();
        if (eksternRef.isPresent()) {
            var ekstern = eksternRef.get();
            var intern = inntektsmeldingBuilder.getInternArbeidsforholdRef();
            if (ekstern.gjelderForSpesifiktArbeidsforhold()) {
                if (intern.isEmpty() || !intern.get().gjelderForSpesifiktArbeidsforhold()) {
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
                .map(Map.Entry::getKey)
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
                .filter(InntektArbeidYtelseGrunnlag::isAktiv)
                .findFirst();
    }

    private InMemoryInntektArbeidYtelseGrunnlagBuilder getGrunnlagBuilder(Long behandlingId, InntektArbeidYtelseAggregatBuilder builder) {
        Objects.requireNonNull(builder, "inntektArbeidYtelserBuilder");
        var opptjeningAggregatBuilder = opprettGrunnlagBuilderFor(behandlingId);
        opptjeningAggregatBuilder.medData(builder);
        return opptjeningAggregatBuilder;
    }

    private Optional<InntektArbeidYtelseGrunnlag> hentInntektArbeidYtelseGrunnlagForBehandling(Long behandlingId) {
        return getAktivtInntektArbeidGrunnlag(behandlingId);
    }

    private void lagreGrunnlag(InntektArbeidYtelseGrunnlag nyttGrunnlag, Long behandlingId) {

        var behGrunnlag = indeksBehandlingTilGrunnlag.computeIfAbsent(behandlingId, k -> new LinkedList<>());

        setField(nyttGrunnlag, "behandlingId", behandlingId);

        behGrunnlag.push(nyttGrunnlag.getEksternReferanse());
        grunnlag.add(nyttGrunnlag);
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

    }
}
