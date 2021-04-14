package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.DelvisTilrettelegging;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.HelTilrettelegging;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.IngenTilrettelegging;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Tilrettelegging;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.TilretteleggingListe;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

@FagsakYtelseTypeRef("SVP")
@BehandlingTypeRef
@ApplicationScoped
public class YtelseSøknadMapper implements SøknadMapper {

    private VirksomhetTjeneste virksomhetTjeneste;

    protected YtelseSøknadMapper() {
    }

    @Inject
    public YtelseSøknadMapper(VirksomhetTjeneste virksomhetTjeneste) {
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    @Override
    public <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker) {
        var søknad = SøknadMapperFelles.mapSøknad(registreringDto, navBruker);

        var dto = (ManuellRegistreringSvangerskapspengerDto) registreringDto;
        var svangerskapspenger = mapTilSvangerskapspenger(dto);
        svangerskapspenger.setMedlemskap(SøknadMapperFelles.mapMedlemskap(registreringDto));
        svangerskapspenger.setOpptjening(SøknadMapperFelles.mapOpptjening(dto, virksomhetTjeneste));
        var omYtelse = new no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory().createOmYtelse();
        omYtelse.getAny().add(new ObjectFactory().createSvangerskapspenger(svangerskapspenger));
        søknad.setOmYtelse(omYtelse);

        søknad.setTilleggsopplysninger(registreringDto.getTilleggsopplysninger());

        return søknad;
    }

    private static Svangerskapspenger mapTilSvangerskapspenger(ManuellRegistreringSvangerskapspengerDto registreringDto) {
        var svangerskapspenger = new Svangerskapspenger();
        svangerskapspenger.setTermindato(registreringDto.getTermindato());
        if (registreringDto.getFoedselsDato() != null && !registreringDto.getFoedselsDato().isEmpty()) {
            svangerskapspenger.setFødselsdato(registreringDto.getFoedselsDato().get(0));
        }
        var tilretteleggingListe = new TilretteleggingListe();
        svangerskapspenger.setTilretteleggingListe(tilretteleggingListe);
        registreringDto.getTilretteleggingArbeidsforhold().forEach(arbeidsforholdDto -> {
            var tilrettelegging = new Tilrettelegging();
            mapTilArbeidsforhold(arbeidsforholdDto, tilrettelegging);
            arbeidsforholdDto.getTilrettelegginger().forEach(tilretteleggingDto -> mapTilTilrettelegging(tilretteleggingDto, tilrettelegging));
            tilrettelegging.setBehovForTilretteleggingFom(arbeidsforholdDto.getBehovsdato());
            tilretteleggingListe.getTilrettelegging().add(tilrettelegging);
        });

        return svangerskapspenger;
    }

    private static void mapTilTilrettelegging(SvpTilretteleggingDto svpTilretteleggingDto, Tilrettelegging tilrettelegging) {
        var tilretteleggingType = svpTilretteleggingDto.getTilretteleggingType();
        switch (tilretteleggingType) {
            case HEL_TILRETTELEGGING -> {
                var helTilrettelegging = new HelTilrettelegging();
                helTilrettelegging.setTilrettelagtArbeidFom(svpTilretteleggingDto.getDato());
                tilrettelegging.getHelTilrettelegging().add(helTilrettelegging);
            }
            case DELVIS_TILRETTELEGGING -> {
                var delvisTilrettelegging = new DelvisTilrettelegging();
                delvisTilrettelegging.setStillingsprosent(svpTilretteleggingDto.getStillingsprosent());
                delvisTilrettelegging.setTilrettelagtArbeidFom(svpTilretteleggingDto.getDato());
                tilrettelegging.getDelvisTilrettelegging().add(delvisTilrettelegging);
            }
            case INGEN_TILRETTELEGGING -> {
                var ingenTilretelegging = new IngenTilrettelegging();
                ingenTilretelegging.setSlutteArbeidFom(svpTilretteleggingDto.getDato());
                tilrettelegging.getIngenTilrettelegging().add(ingenTilretelegging);
            }
            default -> throw new UnsupportedOperationException("Uteglemt enum valg :" + tilretteleggingType);
        }
    }

    private static void mapTilArbeidsforhold(SvpTilretteleggingArbeidsforholdDto arbeidsforholdDto, Tilrettelegging tilrettelegging) {
        if (arbeidsforholdDto instanceof SvpTilretteleggingVirksomhetDto) {
            tilrettelegging.setArbeidsforhold(mapTilArbeidsforhold((SvpTilretteleggingVirksomhetDto) arbeidsforholdDto));
        } else if (arbeidsforholdDto instanceof SvpTilretteleggingPrivatArbeidsgiverDto) {
            tilrettelegging.setArbeidsforhold(mapTilArbeidsforhold((SvpTilretteleggingPrivatArbeidsgiverDto) arbeidsforholdDto));
        } else if (arbeidsforholdDto instanceof SvpTilretteleggingSelvstendigNæringsdrivendeDto) {
            tilrettelegging.setArbeidsforhold(mapTilSelvstendigNæringsdrivende());
        } else if (arbeidsforholdDto instanceof SvpTilretteleggingFrilanserDto) {
            tilrettelegging.setArbeidsforhold(mapTilFrilanser());
        } else {
            throw new IllegalArgumentException("Utvikler-feil: det skal ikke finnes flere konkrete subklasser.");
        }
    }

    private static no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Frilanser mapTilFrilanser() {
        var frilanser = new no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Frilanser();
        // Setter risikofaktor og tilrettelegginstiltak til blank for manuell søknad.
        frilanser.setOpplysningerOmRisikofaktorer("");
        frilanser.setOpplysningerOmTilretteleggingstiltak("");
        return frilanser;
    }

    private static no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.SelvstendigNæringsdrivende mapTilSelvstendigNæringsdrivende() {
        var selvstendigNæringsdrivendeDselvstendigNæringsdrivende = new no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.SelvstendigNæringsdrivende();
        // Setter risikofaktor og tilrettelegginstiltak til blank for manuell søknad.
        selvstendigNæringsdrivendeDselvstendigNæringsdrivende.setOpplysningerOmRisikofaktorer("");
        selvstendigNæringsdrivendeDselvstendigNæringsdrivende.setOpplysningerOmTilretteleggingstiltak("");
        return selvstendigNæringsdrivendeDselvstendigNæringsdrivende;
    }

    private static no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.PrivatArbeidsgiver mapTilArbeidsforhold(SvpTilretteleggingPrivatArbeidsgiverDto privatArbeidsgiverDto) {
        var privatArbeidsgiver = new no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.PrivatArbeidsgiver();
        privatArbeidsgiver.setIdentifikator(privatArbeidsgiverDto.getArbeidsgiverIdentifikator());
        return privatArbeidsgiver;
    }

    private static no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver mapTilArbeidsforhold(SvpTilretteleggingVirksomhetDto virksomhetDto) {
        var arbeidsgiverIdentifikator = virksomhetDto.getOrganisasjonsnummer();
        if (arbeidsgiverIdentifikator == null) {
            throw new IllegalArgumentException("Arbeidsgiver identifikator må være utfylt.");
        }
        arbeidsgiverIdentifikator = arbeidsgiverIdentifikator.trim();
        if (arbeidsgiverIdentifikator.length() == 11) {
            var privatArbeidsgiver = new no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.PrivatArbeidsgiver();
            privatArbeidsgiver.setIdentifikator(arbeidsgiverIdentifikator);
            return privatArbeidsgiver;
        }
        if (arbeidsgiverIdentifikator.length() == 9) {
            var virksomhet = new no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Virksomhet();
            virksomhet.setIdentifikator(arbeidsgiverIdentifikator);
            return virksomhet;
        }
        throw new IllegalArgumentException("Arbeidsgiver identifikator må være 9 eller 11 tegn langt.");
    }

}
