package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class SakInntektsmeldinger {

    private final Map<Key, Set<Inntektsmelding>> data = new LinkedHashMap<>();
    private final Saksnummer saksnummer;

    public SakInntektsmeldinger(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void leggTil(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime grunnlagOpprettetTidspunkt, Inntektsmelding inntektsmelding) {
        data.computeIfAbsent(new Key(behandlingId, grunnlagEksternReferanse, grunnlagOpprettetTidspunkt), k -> new LinkedHashSet<>())
                .add(inntektsmelding);
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    /**
     * Get alle inntektsmelinger for saksnummer.
     */
    public Set<Inntektsmelding> getAlleInntektsmeldinger() {

        Set<Inntektsmelding> inntektsmeldinger = new LinkedHashSet<>();
        for (var entry : data.entrySet()) {
            inntektsmeldinger.addAll(entry.getValue());
        }
        return new HashSet<>(inntektsmeldinger);
    }

    record Key(Long behandlingId, UUID grunnlagEksternReferanse, LocalDateTime opprettetTidspunkt) {

    }

}
