package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class ArbeidsforholdInformasjonBuilder {

    private final ArbeidsforholdInformasjon kladd;

    private ArbeidsforholdInformasjonBuilder(ArbeidsforholdInformasjon kladd) {
        this.kladd = kladd;
    }

    public static ArbeidsforholdInformasjonBuilder oppdatere(ArbeidsforholdInformasjon oppdatere) {
        return new ArbeidsforholdInformasjonBuilder(new ArbeidsforholdInformasjon(oppdatere));
    }

    public ArbeidsforholdOverstyringBuilder getOverstyringBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return kladd.getOverstyringBuilderFor(arbeidsgiver, ref);
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

    public ArbeidsforholdInformasjonBuilder fjernOverstyringVedrørende(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
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
        var arbeidInfo = arbeidsforholdInformasjon.map(ArbeidsforholdInformasjon::new).orElseGet(ArbeidsforholdInformasjon::new);
        return new ArbeidsforholdInformasjonBuilder(arbeidInfo);
    }
}
