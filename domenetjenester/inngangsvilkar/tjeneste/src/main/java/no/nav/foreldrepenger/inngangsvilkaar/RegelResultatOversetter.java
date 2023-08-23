package no.nav.foreldrepenger.inngangsvilkaar;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelEvalueringResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RegelResultatOversetter {

    private static final Map<RegelUtfallMerknad, VilkårUtfallMerknad> UTFALL_MERKNAD_MAP = Map.ofEntries(
        Map.entry(RegelUtfallMerknad.RVM_1001, VilkårUtfallMerknad.VM_1001),
        Map.entry(RegelUtfallMerknad.RVM_1002, VilkårUtfallMerknad.VM_1002),
        Map.entry(RegelUtfallMerknad.RVM_1003, VilkårUtfallMerknad.VM_1003),
        Map.entry(RegelUtfallMerknad.RVM_1004, VilkårUtfallMerknad.VM_1004),
        Map.entry(RegelUtfallMerknad.RVM_1005, VilkårUtfallMerknad.VM_1005),
        Map.entry(RegelUtfallMerknad.RVM_1006, VilkårUtfallMerknad.VM_1006),
        Map.entry(RegelUtfallMerknad.RVM_1019, VilkårUtfallMerknad.VM_1019),
        Map.entry(RegelUtfallMerknad.RVM_1020, VilkårUtfallMerknad.VM_1020),
        Map.entry(RegelUtfallMerknad.RVM_1023, VilkårUtfallMerknad.VM_1023),
        Map.entry(RegelUtfallMerknad.RVM_1024, VilkårUtfallMerknad.VM_1024),
        Map.entry(RegelUtfallMerknad.RVM_1025, VilkårUtfallMerknad.VM_1025),
        Map.entry(RegelUtfallMerknad.RVM_1026, VilkårUtfallMerknad.VM_1026),
        Map.entry(RegelUtfallMerknad.RVM_1027, VilkårUtfallMerknad.VM_1027),
        Map.entry(RegelUtfallMerknad.RVM_1028, VilkårUtfallMerknad.VM_1028),
        Map.entry(RegelUtfallMerknad.RVM_1035, VilkårUtfallMerknad.VM_1035),
        Map.entry(RegelUtfallMerknad.RVM_1051, VilkårUtfallMerknad.VM_1051),
        Map.entry(RegelUtfallMerknad.UDEFINERT, VilkårUtfallMerknad.UDEFINERT));

    private RegelResultatOversetter() {
    }

    public static VilkårData oversett(VilkårType vilkårType, RegelEvalueringResultat resultat) {
        var vilkårUtfallMerknad = Optional.ofNullable(resultat.merknad())
            .map(MerknadRuleReasonRef::regelUtfallMerknad)
            .map(RegelResultatOversetter::mapRegelMerknad).orElse(null);

        return new VilkårData(vilkårType, mapRegelResultUtfallToUtfallType(resultat), vilkårUtfallMerknad, List.of(), resultat.regelEvaluering(),
            resultat.regelInput(), resultat.resultatData());

    }

    private static VilkårUtfallType mapRegelResultUtfallToUtfallType(RegelEvalueringResultat resultat) {
        return switch (resultat.utfall()) {
            case OPPFYLT -> VilkårUtfallType.OPPFYLT;
            case IKKE_OPPFYLT -> VilkårUtfallType.IKKE_OPPFYLT;
            case IKKE_VURDERT -> VilkårUtfallType.IKKE_VURDERT;
        };
    }

    public static VilkårUtfallMerknad mapRegelMerknad(RegelUtfallMerknad merknad) {
        if (merknad == null) {
            return VilkårUtfallMerknad.UDEFINERT;
        }
        if (UTFALL_MERKNAD_MAP.get(merknad) == null) {
            throw new IllegalArgumentException("Utviklerfeil: mangler mapping for regelmerknad " + merknad);
        }
        return UTFALL_MERKNAD_MAP.get(merknad);
    }

}
