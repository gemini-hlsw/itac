-- Response to following schema change.

-- diff -u -N -r37092 -r42072
--- ocs2/osgi/src/bundle/edu.gemini.model.p1/trunk/src-xsd/Meta.xsd	(.../Meta.xsd)	(revision 37092)
-- +++ ocs2/osgi/src/bundle/edu.gemini.model.p1/trunk/src-xsd/Meta.xsd	(.../Meta.xsd)	(revision 42072)
-- @@ -7,6 +7,7 @@
--         <xsd:sequence>
--             <xsd:element name="attachment" type="xsd:string"/>
--         </xsd:sequence>
--+        <xsd:attribute name="band3optionChosen" type="xsd:boolean"/>
--     </xsd:complexType>

ALTER TABLE v2_phase_i_proposals ADD COLUMN band_3_option_chosen BOOLEAN DEFAULT FALSE;