package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;


import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;

public interface KlageVurderingLagreDto {

    KlageVurdering getKlageVurdering();

    KlageMedholdÅrsak getKlageMedholdArsak();

    KlageVurderingOmgjør getKlageVurderingOmgjoer();

    KlageHjemmel getKlageHjemmel();

}
