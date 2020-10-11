package no.nav.foreldrepenger.behandling.steg.innhentsaksopplysninger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.SaksopplysningerFeil;

@BehandlingStegRef(kode = "INPER")
@BehandlingTypeRef("BT-006") // Innsyn
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentPersonopplysningStegImpl implements InnhentRegisteropplysningerSteg {

    private PersoninfoAdapter registerdataInnhenter;

    InnhentPersonopplysningStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentPersonopplysningStegImpl(PersoninfoAdapter registerdataInnhenter) {
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // TODO (essv): Avklare om vi må hente inn mer info om søker (medsøker, barn, +++)
        Personinfo søkerInfo = registerdataInnhenter.innhentSaksopplysningerForSøker(kontekst.getAktørId());
        if (søkerInfo == null) {
            throw SaksopplysningerFeil.FACTORY.feilVedOppslagITPS(kontekst.getAktørId().getId()).toException();
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
