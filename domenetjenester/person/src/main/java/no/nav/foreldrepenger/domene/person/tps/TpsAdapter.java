package no.nav.foreldrepenger.domene.person.tps;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentGeografiskTilknytningSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonhistorikkSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentGeografiskTilknytningResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

@ApplicationScoped
public class TpsAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TpsAdapter.class);

    private AktørTjeneste aktørConsumer;
    private PersonConsumer personConsumer;
    private TpsOversetter tpsOversetter;

    public TpsAdapter() {
    }

    @Inject
    public TpsAdapter(AktørTjeneste aktørConsumer,
                      PersonConsumer personConsumer,
                      TpsOversetter tpsOversetter) {
        this.aktørConsumer = aktørConsumer;
        this.personConsumer = personConsumer;
        this.tpsOversetter = tpsOversetter;
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        return aktørConsumer.hentAktørIdForPersonIdent(personIdent);
    }

    public Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId);
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

    private Personinfo håndterPersoninfoRespons(AktørId aktørId, HentPersonRequest request)
        throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonResponse response = personConsumer.hentPersonResponse(request);
        Person person = response.getPerson();
        if (!(person instanceof Bruker)) {
            throw TpsFeilmeldinger.FACTORY.ukjentBrukerType().toException();
        }
        return tpsOversetter.tilBrukerInfo(aktørId, (Bruker) person);
    }

    private Personhistorikkinfo håndterPersonhistorikkRespons(HentPersonhistorikkRequest request, String aktørId)
        throws HentPersonhistorikkSikkerhetsbegrensning, HentPersonhistorikkPersonIkkeFunnet {
        HentPersonhistorikkResponse response = personConsumer.hentPersonhistorikkResponse(request);
        return tpsOversetter.tilPersonhistorikkInfo(aktørId, response);
    }

    public Personinfo hentKjerneinformasjon(PersonIdent personIdent, AktørId aktørId) {
        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(TpsUtil.lagPersonIdent(personIdent.getIdent()));
        request.getInformasjonsbehov().add(Informasjonsbehov.ADRESSE);
        request.getInformasjonsbehov().add(Informasjonsbehov.KOMMUNIKASJON);
        request.getInformasjonsbehov().add(Informasjonsbehov.FAMILIERELASJONER);
        try {
            return håndterPersoninfoRespons(aktørId, request);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

    public Personhistorikkinfo hentPersonhistorikk(AktørId aktørId, Interval interval) {
        HentPersonhistorikkRequest request = new HentPersonhistorikkRequest();
        AktoerId aktoerId = new AktoerId();
        aktoerId.setAktoerId(aktørId.getId());
        Periode periode = new Periode();

        periode.setTom(DateUtil.convertToXMLGregorianCalendar(LocalDateTime.ofInstant(interval.getEnd(), ZoneId.systemDefault())));
        periode.setFom(DateUtil.convertToXMLGregorianCalendar(LocalDateTime.ofInstant(interval.getStart(), ZoneId.systemDefault())));

        request.setAktoer(aktoerId);
        request.setPeriode(periode);

        try {
            return håndterPersonhistorikkRespons(request, String.valueOf(aktørId));
        } catch (HentPersonhistorikkPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePersonhistorikkForAktørId(e).toException();
        } catch (HentPersonhistorikkSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }

}
