package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.ReferanseType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class MapTilOpptjeningAktiviteter {

    public MapTilOpptjeningAktiviteter() {
    }

    public List<OpptjeningAktivitet> map(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder,
            OpptjeningAktivitetKlassifisering klassifisering) {

        List<OpptjeningAktivitet> opptjeningAktivitet = new ArrayList<>();
        for (var entry : perioder.entrySet()) {
            for (var seg : entry.getValue().toSegments()) {
                var key = entry.getKey();
                var aktType = OpptjeningAktivitetType.fraKode(key.getAktivitetType());
                var aktivitetReferanse = key.getAktivitetReferanse();
                var refType = getAktivitetReferanseType(aktivitetReferanse, key);

                var oppAkt = new OpptjeningAktivitet(seg.getFom(), seg.getTom(), aktType, klassifisering,
                        aktivitetReferanse, refType);
                opptjeningAktivitet.add(oppAkt);
            }
        }
        return opptjeningAktivitet;
    }

    private ReferanseType getAktivitetReferanseType(String aktivitetReferanse, Aktivitet key) {
        if (aktivitetReferanse != null) {
            if (key.getReferanseType() == Aktivitet.ReferanseType.ORGNR) {
                return ReferanseType.ORG_NR;
            }
            if (key.getReferanseType() == Aktivitet.ReferanseType.AKTØRID) {
                return ReferanseType.AKTØR_ID;
            }
            throw new IllegalArgumentException(
                    "Utvikler-feil: Mangler aktivitetReferanseType for aktivitetReferanse["
                            + key.getReferanseType()
                            + "]: "
                            + aktivitetReferanse);

        }

        return null;
    }
}
