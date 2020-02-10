package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
        data.computeIfAbsent(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), k -> new LinkedHashSet<>()).add(inntektsmelding);
    }

    public void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, InntektArbeidYtelseGrunnlag grunnlag) {
        this.grunnlag.put(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), grunnlag);
    }

    public Optional<UUID> getSisteGrunnlagReferanseDerInntektsmeldingerForskjelligFraNyeste(Long behandlingId) {
        List<Key> grunnlagDesc = data.keySet().stream()
            .sorted(Comparator.comparing(Key::getOpprettetTidspunkt, Comparator.nullsLast(Comparator.reverseOrder())))
            .distinct()
            .filter(k -> Objects.equals(k.behandlingId, behandlingId))
            .collect(Collectors.toList());

        if (grunnlagDesc.size() >= 2) {
            Key første = grunnlagDesc.get(0);
            Set<Inntektsmelding> førsteInntektsmeldinger = data.get(første);
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
        List<Key> grunnlagDesc = grunnlag.keySet().stream()
            .distinct()
            .filter(k -> Objects.equals(k.behandlingId, behandlingId))
            .filter(k -> Objects.equals(k.grunnlagEksternReferanse, grunnlagRef))
            .collect(Collectors.toList());

        if (grunnlagDesc.size() == 1) {
            return Optional.of(grunnlag.get(grunnlagDesc.get(0)));
        } else if (grunnlagDesc.isEmpty()) {
            return Optional.empty();
        }
        throw new IllegalStateException("Flere grunnlag med samme referanse");
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    /**
     * Get alle inntektsmelinger for saksnummer. Returneres i rekkefølge innsendingstidspunkt (eldste først).
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

    static class Key {
        final Long behandlingId;
        final UUID grunnlagEksternReferanse;
        final LocalDateTime opprettetTidspunkt;

        Key(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime opprettetTidspunkt) {
            this.behandlingId = behandlingId;
            this.grunnlagEksternReferanse = grunnlagEksternReferanse;
            this.opprettetTidspunkt = opprettetTidspunkt;
        }

        LocalDateTime getOpprettetTidspunkt() {
            return opprettetTidspunkt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(behandlingId, key.behandlingId) &&
                Objects.equals(grunnlagEksternReferanse, key.grunnlagEksternReferanse) &&
                Objects.equals(opprettetTidspunkt, key.opprettetTidspunkt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(behandlingId, grunnlagEksternReferanse, opprettetTidspunkt);
        }
    }

}
