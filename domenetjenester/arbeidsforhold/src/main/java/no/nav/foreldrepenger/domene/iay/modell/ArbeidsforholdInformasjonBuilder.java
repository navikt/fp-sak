package no.nav.foreldrepenger.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.Tuple;

public class ArbeidsforholdInformasjonBuilder {

    private final ArbeidsforholdInformasjon kladd;
    private final List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> erstattArbeidsforhold = new ArrayList<>();
    private final List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> reverserteErstattninger = new ArrayList<>();

    private ArbeidsforholdInformasjonBuilder(ArbeidsforholdInformasjon kladd) {
        this.kladd = kladd;
    }

    public static ArbeidsforholdInformasjonBuilder oppdatere(ArbeidsforholdInformasjon oppdatere) {
        return new ArbeidsforholdInformasjonBuilder(new ArbeidsforholdInformasjon(oppdatere));
    }

    public ArbeidsforholdOverstyringBuilder getOverstyringBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return kladd.getOverstyringBuilderFor(arbeidsgiver, ref);
    }

    public ArbeidsforholdInformasjonBuilder tilbakestillOverstyringer() {
        final List<ArbeidsforholdReferanse> collect = kladd.getArbeidsforholdReferanser().stream().filter(it -> kladd.getOverstyringer().stream()
            .anyMatch(ov -> ov.getHandling().equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                && ov.getNyArbeidsforholdRef().gjelderFor(it.getInternReferanse())))
            .collect(Collectors.toList());
        collect.forEach(it -> {
            Optional<InternArbeidsforholdRef> arbeidsforholdRef = kladd.finnForEksternBeholdHistoriskReferanse(it.getArbeidsgiver(), it.getEksternReferanse());
            if (arbeidsforholdRef.isPresent()) {
                reverserteErstattninger.add(new Tuple<>(it.getArbeidsgiver(), new Tuple<>(it.getInternReferanse(), arbeidsforholdRef.get())));
            }
        });
        kladd.tilbakestillOverstyringer();
        return this;
    }

    /**
     * Benyttes for å vite hvilke inntektsmeldinger som skal tas ut av grunnlaget ved erstatting av ny id.
     *
     * @return Liste over Arbeidsgiver / ArbeidsforholdReferanser
     */
    public List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> getErstattArbeidsforhold() {
        return Collections.unmodifiableList(erstattArbeidsforhold);
    }

    /**
     * Benyttes for å vite hvilke inntektsmeldinger som skal tas ut av grunnlaget ved erstatting av ny id.
     *
     * @return Liste over Arbeidsgiver / ArbeidsforholdReferanser
     */
    public List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> getReverserteErstattArbeidsforhold() {
        return Collections.unmodifiableList(reverserteErstattninger);
    }

    public ArbeidsforholdInformasjonBuilder erstattArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef gammelRef, InternArbeidsforholdRef ref) {
        // TODO: Sjekke om revertert allerede
        // Hvis eksisterer så reverter revertering og ikke legg inn erstattning og kall på erstatt
        erstattArbeidsforhold.add(new Tuple<>(arbeidsgiver, new Tuple<>(gammelRef, ref)));
        kladd.erstattArbeidsforhold(arbeidsgiver, gammelRef, ref);
        return this;
    }

    public ArbeidsforholdInformasjonBuilder leggTil(ArbeidsforholdOverstyringBuilder overstyringBuilder) {
        if (!overstyringBuilder.isOppdatering()) {
            kladd.leggTilOverstyring(overstyringBuilder.build());
        }
        return this;
    }

    public ArbeidsforholdInformasjon build() {
        return kladd;
    }

    public ArbeidsforholdInformasjonBuilder fjernOverstyringVedrørende(Arbeidsgiver arbeidsgiver,
                                                                       InternArbeidsforholdRef arbeidsforholdRef) {
        kladd.fjernOverstyringVedrørende(arbeidsgiver, arbeidsforholdRef);
        return this;
    }

    public void fjernAlleOverstyringer() {
        kladd.tilbakestillOverstyringer();
    }

    public void fjernOverstyringerSomGjelder(Arbeidsgiver arbeidsgiver) {
        kladd.fjernOverstyringerSomGjelder(arbeidsgiver);
    }

    public void leggTil(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse, EksternArbeidsforholdRef eksternReferanse) {
        kladd.leggTilNyReferanse(new ArbeidsforholdReferanse(arbeidsgiver, internReferanse, eksternReferanse));
    }

    public void leggTilNyReferanse(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        kladd.leggTilNyReferanse(arbeidsforholdReferanse);
    }

    public static ArbeidsforholdInformasjonBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        return oppdatere(InntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag).getInformasjon());
    }

    public static ArbeidsforholdInformasjonBuilder builder(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon) {
        var arbeidInfo = arbeidsforholdInformasjon.map(ai -> new ArbeidsforholdInformasjon(ai)).orElseGet(() -> new ArbeidsforholdInformasjon());
        return new ArbeidsforholdInformasjonBuilder(arbeidInfo);
    }
}
