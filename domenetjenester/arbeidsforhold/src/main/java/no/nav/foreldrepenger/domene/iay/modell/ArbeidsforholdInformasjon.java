package no.nav.foreldrepenger.domene.iay.modell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.OrderBy;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class ArbeidsforholdInformasjon extends BaseEntitet {



    @ChangeTracked
    @OrderBy("opprettetTidspunkt ASC")
    private Set<ArbeidsforholdReferanse> referanser = new LinkedHashSet<>();

    @ChangeTracked
    private List<ArbeidsforholdOverstyring> overstyringer = new ArrayList<>();

    public ArbeidsforholdInformasjon() {
    }

    public ArbeidsforholdInformasjon(ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        for (ArbeidsforholdReferanse arbeidsforholdReferanse : arbeidsforholdInformasjon.referanser) {
            final ArbeidsforholdReferanse referanseEntitet = new ArbeidsforholdReferanse(arbeidsforholdReferanse);
            this.referanser.add(referanseEntitet);
        }
        for (ArbeidsforholdOverstyring arbeidsforholdOverstyringEntitet : arbeidsforholdInformasjon.overstyringer) {
            final ArbeidsforholdOverstyring overstyringEntitet = new ArbeidsforholdOverstyring(arbeidsforholdOverstyringEntitet);
            this.overstyringer.add(overstyringEntitet);
        }
    }

    public Collection<ArbeidsforholdReferanse> getArbeidsforholdReferanser() {
        return Collections.unmodifiableSet(this.referanser);
    }

    public List<ArbeidsforholdOverstyring> getOverstyringer() {
        return Collections.unmodifiableList(this.overstyringer);
    }

    public EksternArbeidsforholdRef finnEkstern(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
        if (internReferanse.getReferanse() == null) return EksternArbeidsforholdRef.nullRef();

        return referanser.stream()
            .filter(this::erIkkeMerget)
            .filter(r -> Objects.equals(r.getInternReferanse(), internReferanse) && Objects.equals(r.getArbeidsgiver(), arbeidsgiver))
            .findFirst()
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .orElseThrow(
                () -> new IllegalStateException("Mangler eksternReferanse for internReferanse: " + internReferanse + ", arbeidsgiver: " + arbeidsgiver));
    }

    /** @deprecated Bruk {@link ArbeidsforholdInformasjon#finnEkstern} i stedet. */
    @Deprecated(forRemoval = true)
    public EksternArbeidsforholdRef finnEksternRaw(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse) {
        if (internReferanse.getReferanse() == null) return EksternArbeidsforholdRef.nullRef();

        return referanser.stream()
            .filter(r -> Objects.equals(r.getInternReferanse(), internReferanse) && Objects.equals(r.getArbeidsgiver(), arbeidsgiver))
            .findFirst()
            .map(ArbeidsforholdReferanse::getEksternReferanse)
            .orElseThrow(
                () -> new IllegalStateException("Mangler eksternReferanse for internReferanse: " + internReferanse + ", arbeidsgiver: " + arbeidsgiver));
    }

    public Optional<InternArbeidsforholdRef> finnForEksternBeholdHistoriskReferanse(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef arbeidsforholdRef) {
        // For å sike at det ikke mistes data ved sammenslåing av og innhenting av registerdataop
        final Optional<ArbeidsforholdReferanse> referanseEntitet = referanser.stream().filter(re -> overstyringer.stream()
            .anyMatch(ov -> ov.getHandling().equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                && ov.getArbeidsgiver().equals(arbeidsgiver)
                && re.getArbeidsgiver().equals(ov.getArbeidsgiver())
                && re.getEksternReferanse().equals(arbeidsforholdRef)
                && re.getInternReferanse().equals(ov.getArbeidsforholdRef())))
            .findAny();
        if (referanseEntitet.isPresent()) {
            return Optional.ofNullable(referanseEntitet.get().getInternReferanse());
        }
        return finnForEkstern(arbeidsgiver, arbeidsforholdRef);
    }

    public Optional<InternArbeidsforholdRef> finnForEkstern(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {
        final List<ArbeidsforholdReferanse> arbeidsforholdReferanseEntitetStream = this.referanser.stream()
            .filter(this::erIkkeMerget)
            .collect(Collectors.toList());
        return arbeidsforholdReferanseEntitetStream.stream()
            .filter(it -> it.getArbeidsgiver().equals(arbeidsgiver) && it.getEksternReferanse().equals(ref))
            .findFirst().map(ArbeidsforholdReferanse::getInternReferanse);
    }

    private boolean erIkkeMerget(ArbeidsforholdReferanse arbeidsforholdReferanseEntitet) {
        return overstyringer.stream().noneMatch(ov -> ov.getHandling().equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
            && ov.getArbeidsgiver().equals(arbeidsforholdReferanseEntitet.getArbeidsgiver())
            && ov.getArbeidsforholdRef().equals(arbeidsforholdReferanseEntitet.getInternReferanse()));
    }

    /**
     * @deprecated FIXME (FC): Trengs denne eller kan vi alltid stole på ref er den vi skal returnere? Skal egentlig returnere ref,
     * men per nå har vi antagelig interne ider som har erstattet andre interne id'er. Må isåfall avsjekke migrering av disse.
     */
    @Deprecated(forRemoval = true)
    public InternArbeidsforholdRef finnEllerOpprett(Arbeidsgiver arbeidsgiver, final InternArbeidsforholdRef ref) {
        final Optional<ArbeidsforholdOverstyring> erstattning = overstyringer.stream()
            .filter(ov -> ov.getHandling().equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                && ov.getArbeidsgiver().equals(arbeidsgiver)
                && ov.getArbeidsforholdRef().gjelderFor(ref))
            .findAny();
        if (erstattning.isPresent() && !erstattning.get().getNyArbeidsforholdRef().equals(ref)) {
            return finnEllerOpprett(arbeidsgiver, erstattning.get().getNyArbeidsforholdRef());
        } else {
            final ArbeidsforholdReferanse referanse = this.referanser.stream()
                .filter(this::erIkkeMerget)
                .filter(it -> it.getArbeidsgiver().equals(arbeidsgiver) && it.getInternReferanse().equals(ref))
                .findFirst().orElseThrow(() -> new IllegalStateException("InternArbeidsforholdReferanse må eksistere fra før, fant ikke: " + ref));

            return referanse.getInternReferanse();
        }
    }

    public InternArbeidsforholdRef finnEllerOpprett(Arbeidsgiver arbeidsgiver, final EksternArbeidsforholdRef ref) {
        final Optional<ArbeidsforholdOverstyring> erstattning = overstyringer.stream()
            .filter(ov -> {
                var historiskReferanse = finnForEksternBeholdHistoriskReferanse(arbeidsgiver, ref);
                return historiskReferanse.isPresent()
                    && ov.getHandling().equals(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
                    && ov.getArbeidsgiver().equals(arbeidsgiver)
                    && ov.getArbeidsforholdRef().gjelderFor(historiskReferanse.get());
            })
            .findAny();
        if (erstattning.isPresent()) {
            return finnEllerOpprett(arbeidsgiver, erstattning.get().getNyArbeidsforholdRef());
        } else {
            final ArbeidsforholdReferanse referanse = finnEksisterendeInternReferanseEllerOpprettNy(arbeidsgiver, ref);
            return referanse.getInternReferanse();
        }
    }

    private Optional<ArbeidsforholdReferanse> referanseEksistererIkke(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return this.referanser.stream()
            .filter(this::erIkkeMerget)
            .filter(it -> it.getArbeidsgiver().equals(arbeidsgiver) && it.getInternReferanse().equals(ref))
            .findAny();
    }

    private ArbeidsforholdReferanse finnEksisterendeInternReferanseEllerOpprettNy(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef eksternReferanse) {
        return finnEksisterendeReferanse(arbeidsgiver, eksternReferanse)
            .orElseGet(() -> opprettNyReferanse(arbeidsgiver, InternArbeidsforholdRef.nyRef(), eksternReferanse));
    }

    private Optional<ArbeidsforholdReferanse> finnEksisterendeReferanse(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {
        return this.referanser.stream()
            .filter(this::erIkkeMerget)
            .filter(it -> it.getArbeidsgiver().equals(arbeidsgiver) && it.getEksternReferanse().equals(ref))
            .findAny();
    }

    /**
     * @deprecated Bruk {@link ArbeidsforholdInformasjonBuilder} i stedet.
     */
    @Deprecated(forRemoval = true)
    public ArbeidsforholdReferanse opprettNyReferanse(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internReferanse,
                                                             EksternArbeidsforholdRef eksternReferanse) {
        final ArbeidsforholdReferanse arbeidsforholdReferanse = new ArbeidsforholdReferanse(arbeidsgiver,
            internReferanse, eksternReferanse);
        referanser.add(arbeidsforholdReferanse);
        return arbeidsforholdReferanse;
    }

    void leggTilNyReferanse(ArbeidsforholdReferanse arbeidsforholdReferanse) {
        referanser.add(arbeidsforholdReferanse);
    }

    ArbeidsforholdOverstyringBuilder getOverstyringBuilderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return ArbeidsforholdOverstyringBuilder.oppdatere(this.overstyringer
            .stream()
            .filter(ov -> ov.getArbeidsgiver().equals(arbeidsgiver)
                && ov.getArbeidsforholdRef().gjelderFor(ref))
            .findFirst())
            .medArbeidsforholdRef(ref)
            .medArbeidsgiver(arbeidsgiver);
    }

    void leggTilOverstyring(ArbeidsforholdOverstyring build) {
        this.overstyringer.add(build);
    }

    void tilbakestillOverstyringer() {
        this.overstyringer.clear();
    }

    void fjernOverstyringerSomGjelder(Arbeidsgiver arbeidsgiver) {
        this.overstyringer.removeIf(ov -> arbeidsgiver.equals(ov.getArbeidsgiver()));
    }

    void erstattArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef gammelRef, InternArbeidsforholdRef ref) {
        final Optional<ArbeidsforholdReferanse> referanseEntitet = referanseEksistererIkke(arbeidsgiver, gammelRef);
        referanseEntitet.ifPresent(it -> opprettNyReferanse(arbeidsgiver, ref, it.getEksternReferanse()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof ArbeidsforholdInformasjon))
            return false;
        ArbeidsforholdInformasjon that = (ArbeidsforholdInformasjon) o;
        return Objects.equals(referanser, that.referanser) &&
            Objects.equals(overstyringer, that.overstyringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanser, overstyringer);
    }

    @Override
    public String toString() {
        return "ArbeidsforholdInformasjonEntitet{" +
            "referanser=" + referanser +
            ", overstyringer=" + overstyringer +
            '}';
    }

    void fjernOverstyringVedrørende(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        overstyringer.removeIf(ov -> !Objects.equals(ov.getHandling(), ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET)
            && ov.getArbeidsgiver().equals(arbeidsgiver)
            && ov.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef));
    }
}
