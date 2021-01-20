package no.nav.foreldrepenger.domene.person.tps;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

@ApplicationScoped
public class TpsAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TpsAdapter.class);

    private PersonConsumer personConsumer;

    public TpsAdapter() {
    }

    @Inject
    public TpsAdapter(PersonConsumer personConsumer) {
        this.personConsumer = personConsumer;
    }

    // Last method standing ....
    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent personIdent) {
        HentGeografiskTilknytningRequest request = new HentGeografiskTilknytningRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        try {
            HentGeografiskTilknytningResponse response = personConsumer.hentGeografiskTilknytning(request);
            String geoTilkn = response.getGeografiskTilknytning() != null
                ? response.getGeografiskTilknytning().getGeografiskTilknytning()
                : null;
            String diskKode = response.getDiskresjonskode() != null ? response.getDiskresjonskode().getValue() : null;

            return new GeografiskTilknytning(geoTilkn, Diskresjonskode.finnForKodeverkEiersKode(diskKode));
        } catch (HentGeografiskTilknytningSikkerhetsbegrensing e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligGeografiskTilknytningSikkerhetsbegrensing(e).toException();
        } catch (HentGeografiskTilknytningPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.geografiskTilknytningIkkeFunnet(e).toException();
        }
    }

}
