package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class SakInntektsmeldinger {

    private final Map<Key, Set<Inntektsmelding>> data = new LinkedHashMap<>();
    private final Map<Key, InntektArbeidYtelseGrunnlag> grunnlag = new LinkedHashMap<>();
    private Saksnummer saksnummer;

    public SakInntektsmeldinger(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, Inntektsmelding inntektsmelding) {
        data.computeIfAbsent(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), k -> new LinkedHashSet<>())
                .add(inntektsmelding);
    }

    public void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt,
            InntektArbeidYtelseGrunnlag grunnlag) {
        this.grunnlag.put(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), grunnlag);
    }

    public Optional<UUID> getSisteGrunnlagReferanseDerInntektsmeldingerForskjelligFraNyeste(Long behandlingId) {
        var grunnlagDesc = data.keySet().stream()
                .sorted(Comparator.comparing(Key::opprettetTidspunkt, Comparator.nullsLast(Comparator.reverseOrder())))
                .distinct()
                .filter(k -> Objects.equals(k.behandlingId, behandlingId))
                .collect(Collectors.toList());

        if (grunnlagDesc.size() >= 2) {
            var første = grunnlagDesc.get(0);
            var førsteInntektsmeldinger = data.get(første);
            for (var key : grunnlagDesc.subList(1, grunnlagDesc.size())) {
                if (!Objects.equals(førsteInntektsmeldinger, data.get(key))) {
                    return Optional.of(key.grunnlagEksternReferanse);
                }
            }

        }
        return Optional.empty();
    }

    public Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId, UUID grunnlagRef) {
        Objects.requireNonNull(grunnlagRef, "grunnlagRef");
        var grunnlagDesc = grunnlag.keySet().stream()
                .distinct()
                .filter(k -> Objects.equals(k.behandlingId, behandlingId))
                .filter(k -> Objects.equals(k.grunnlagEksternReferanse, grunnlagRef))
                .collect(Collectors.toList());

        if (grunnlagDesc.size() == 1) {
            return Optional.of(grunnlag.get(grunnlagDesc.get(0)));
        }
        if (grunnlagDesc.isEmpty()) {
            return Optional.empty();
        }
        throw new IllegalStateException("Flere grunnlag med samme referanse");
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    /**
     * Get alle inntektsmelinger for saksnummer. Returneres i rekkefølge
     * innsendingstidspunkt (eldste først).
     */
    public Set<Inntektsmelding> getAlleInntektsmeldinger() {

        Set<Inntektsmelding> inntektsmeldinger = new LinkedHashSet<>();
        for (var entry : data.entrySet()) {
            inntektsmeldinger.addAll(entry.getValue());
        }
        var sorted = inntektsmeldinger.stream()
                .sorted(Comparator.comparing(Inntektsmelding::getInnsendingstidspunkt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toSet());
        return sorted;
    }

    record Key(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime opprettetTidspunkt) {

    }

}
