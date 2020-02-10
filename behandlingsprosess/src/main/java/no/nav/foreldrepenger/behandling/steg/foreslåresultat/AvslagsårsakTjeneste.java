package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class AvslagsårsakTjeneste {

    @Inject
    public AvslagsårsakTjeneste() {
    }

    public Avslagsårsak finnAvslagsårsak(Vilkår vilkår) {
        Avslagsårsak avslagsårsak = vilkår.getGjeldendeAvslagsårsak();
        if (avslagsårsak == null) {
            if (vilkår.getVilkårUtfallMerknad() == null) {

                Set<Avslagsårsak> avslagsårsaker = vilkår.getVilkårType().getAvslagsårsaker();
                if (avslagsårsaker.size() != 1) {
                    throw AvslagsårsakFeil.FEILFACTORY
                        .kanIkkeUtledeAvslagsårsakUtfallMerknadMangler(vilkår.getVilkårType().getKode()).toException();
                } else {
                    return avslagsårsaker.iterator().next();
                }
            }
            avslagsårsak = Avslagsårsak.fraKode(vilkår.getVilkårUtfallMerknad().getKode());
            if (avslagsårsak == null) {
                throw AvslagsårsakFeil.FEILFACTORY
                    .kanIkkeUtledeAvslagsårsakFraUtfallMerknad(vilkår.getVilkårUtfallMerknad().getKode())
                    .toException();
            }
        }
        return avslagsårsak;
    }

    private interface AvslagsårsakFeil extends DeklarerteFeil {
        AvslagsårsakFeil FEILFACTORY = FeilFactory.create(AvslagsårsakFeil.class); // NOSONAR ok med konstant
        // i interface her

        @TekniskFeil(feilkode = "FP-411110", feilmelding = "Kan ikke utlede avslagsårsak fra utfallmerknad %s.", logLevel = LogLevel.ERROR)
        Feil kanIkkeUtledeAvslagsårsakFraUtfallMerknad(String kode);

        @TekniskFeil(feilkode = "FP-411111", feilmelding = "Kan ikke utlede avslagsårsak, utfallmerknad mangler i vilkår %s.", logLevel = LogLevel.ERROR)
        Feil kanIkkeUtledeAvslagsårsakUtfallMerknadMangler(String kode);
    }
}
